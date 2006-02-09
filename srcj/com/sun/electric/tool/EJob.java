/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EJob.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool;

import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job.Type;
import java.awt.geom.Point2D;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Class to track Job serializing and execution.
 */
class EJob {
    
    enum State {
        /** waiting on client */                CLIENT_WAITING,
        /** waiting on server */                WAITING,
        /** running on server */                RUNNING,
        /** done on server */                   SERVER_DONE,
        /** done on client */                   CLIENT_DONE;
    };
    
    /*private*/ final static String WAITING_NOW = "waiting now";
    /*private*/ final static String ABORTING = "aborting";
    
    ServerConnection connection;
    int jobId;
    /** type of job (change or examine) */      final Type jobType;
    /** name of job */                          final String jobName;
    State state;
    /** progress */                             /*private*/ String progress = null;
    byte[] serializedJob;
    byte[] serializedResult;
    Job serverJob;
    Job clientJob;
    /** list of saved Highlights */             List<Object> savedHighlights;
    /** saved Highlight offset */               Point2D savedHighlightsOffset;
    /** Fields changed on server side. */       ArrayList<Field> changedFields;
    
    /** Creates a new instance of EJob */
    EJob(ServerConnection connection, int jobId, Job.Type jobType, String jobName, byte[] bytes) {
        this.connection = connection;
        this.jobId = jobId;
        this.jobType = jobType;
        this.jobName = jobName;
        state = State.WAITING;
        serializedJob = bytes;
    }
    
    EJob(Job job, Job.Type jobType, String jobName) {
        this.jobType = jobType;
        this.jobName = jobName;
        state = State.CLIENT_WAITING;
        serverJob = clientJob = job;
        savedHighlights = new ArrayList<Object>();
        if (jobType == Job.Type.CHANGE || jobType == Job.Type.UNDO)
            saveHighlights();
    }
    
    Job getJob() { return clientJob != null ? clientJob : serverJob; }
    
    boolean isExamine() {
        return jobType == Job.Type.EXAMINE || jobType == Job.Type.REMOTE_EXAMINE;
    }   
    
    Throwable serialize() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            out.writeObject(clientJob);
            out.flush();
            serializedJob = byteStream.toByteArray();
            return null;
        } catch (Throwable e) {
            return e;
        }
    }
    
    Throwable deserialize() {
        try {
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedJob));
            Job job = (Job)in.readObject();
            in.close();
            job.ejob = this;
            serverJob = job;
            return null;
        } catch (Throwable e) {
            return e;
        }
    }
    
    void serializeResult() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            out.writeObject(null); // No exception
            out.writeInt(changedFields.size());
            for (Field f: changedFields) {
                Object value = f.get(serverJob);
                out.writeUTF(f.getName());
                out.writeObject(value);
            }
            out.close();
            serializedResult = byteStream.toByteArray();
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "serializeResult", "failure", e);
            serializeExceptionResult(e);
        }
    }
    
    void serializeExceptionResult(Throwable jobException) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            jobException.getStackTrace();
            out.writeObject(jobException);
            out.writeInt(0);
            out.close();
            serializedResult = byteStream.toByteArray();
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "serializeExceptionResult", "failure", e);
            serializedResult = new byte[0];
        }
    }

        
    Throwable deserializeResult() {
        try {
            Class jobClass = clientJob.getClass();
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedResult));
            Throwable jobException = (Throwable)in.readObject();
            int numFields = in.readInt();
            for (int i = 0; i < numFields; i++) {
                String fieldName = in.readUTF();
                Object value = in.readObject();
                Field f = findField(fieldName);
                f.setAccessible(true);
                f.set(clientJob, value);
            }
            in.close();
            return jobException;
        } catch (Throwable e) {
            Job.logger.logp(Level.WARNING, getClass().getName(), "deserializeResult", "failure", e);
            return e;
        }
    }
    
    /**
     * Method to remember that a field variable of the Job has been changed by the doIt() method.
     * @param fieldName the name of the variable that changed.
     */
    protected void fieldVariableChanged(String fieldName) {
        Field fld = findField(fieldName);
        fld.setAccessible(true);
        changedFields.add(fld);
    }
    
    private Field findField(String fieldName) {
        Class jobClass = getJob().getClass();
        Field fld = null;
        while (jobClass != Job.class) {
            try {
                return jobClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                jobClass = jobClass.getSuperclass();
            }
        }
        return null;
    }

    /** Save current Highlights */
    private void saveHighlights() {
        savedHighlights.clear();

        // for now, just save highlights in current window
        UserInterface ui = Job.getUserInterface();
        if (ui == null) return;
        EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

        savedHighlights = wnd.saveHighlightList();
        savedHighlightsOffset = wnd.getHighlightOffset();
    }

    Event newEvent() { return new Event(state); }
    
    class Event {
        State newState;
        Event(State newState) { this.newState = newState; }
        EJob getEJob() { return EJob.this; }
    }
}
