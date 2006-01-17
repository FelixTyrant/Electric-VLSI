/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Job.java
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

import com.sun.electric.Main;
import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.EObjectOutputStream;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.SnapshotReader;
import com.sun.electric.database.SnapshotWriter;
import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.CantEditException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Jobs are processes that will run in the background, such as 
 * DRC, NCC, Netlisters, etc.  Each Job gets placed in a Job
 * window, and reports its status.  A job can be cancelled.
 * 
 * <p>To start a new job, do:
 * <p>Job job = new Job(name);
 * <p>job.start();
 * 
 * <p>The extending class must implement doIt(), which does the job and
 * returns status (true on success; false on failure);
 * and getProgress(), which returns a string indicating the current status.
 * Job also contains boolean abort, which gets set when the user decides
 * to abort the Job.  The extending class' code should check abort when/where
 * applicable.
 *
 * <p>Note that if your Job calls methods outside of this thread
 * that access shared data, those called methods should be synchronized.
 *
 * @author  gainsley
 */
public abstract class Job implements Serializable {

    private static boolean DEBUG = false;
    private static boolean GLOBALDEBUG = false;
    private static Mode threadMode;
    private static int socketPort = 35742; // socket port for client/server
    public static boolean BATCHMODE = false; // to run it in batch mode
    public static boolean NOTHREADING = false;             // to turn off Job threading
    public static boolean LOCALDEBUGFLAG; // Gilda's case
    
    /**
	 * Method to tell whether Electric is running in "debug" mode.
	 * If the program is started with the "-debug" switch, debug mode is enabled.
	 * @return true if running in debug mode.
	 */
    public static boolean getDebug() { return GLOBALDEBUG; }

    public static void setDebug(boolean f) { GLOBALDEBUG = f; }

    /**
     * Mode of Job manager
     */
    public static enum Mode {
        /** Full screen run of Electric. */         FULL_SCREEN,
        /** Batch mode. */                          BATCH,
        /** Server side. */                         SERVER,
        /** Client side. */                         CLIENT;
    }
    
    /**
	 * Type is a typesafe enum class that describes the type of job (CHANGE or EXAMINE).
	 */
	public static enum Type {
		/** Describes a database change. */			CHANGE,
		/** Describes a database undo/redo. */      UNDO,
		/** Describes a database examination. */	EXAMINE;
	}


	/**
	 * Priority is a typesafe enum class that describes the priority of a job.
	 */
	public static class Priority
	{
		private final String name;
		private final int level;

		private Priority(String name, int level) { this.name = name;   this.level = level; }

		/**
		 * Returns a printable version of this Priority.
		 * @return a printable version of this Priority.
		 */
		public String toString() { return name; }

		/**
		 * Returns a level of this Priority.
		 * @return a level of this Priority.
		 */
		public int getLevel() { return level; }

		/** The highest priority: from the user. */		public static final Priority USER         = new Priority("user", 1);
		/** Next lower priority: visible changes. */	public static final Priority VISCHANGES   = new Priority("visible-changes", 2);
		/** Next lower priority: invisible changes. */	public static final Priority INVISCHANGES = new Priority("invisble-changes", 3);
		/** Lowest priority: analysis. */				public static final Priority ANALYSIS     = new Priority("analysis", 4);
	}

    public static Iterator<Job> getDatabaseThreadJobs() {
        return databaseChangesThread.getAllJobs();
    }

	/**
	 * Thread which execute all database change Jobs.
	 */
	private static class DatabaseChangesThread extends Thread
	{
        // Job Management
        /** started jobs */                         private static final ArrayList<Job> startedJobs = new ArrayList<Job>();
        /** waiting jobs */                         private static final ArrayList<Job> waitingJobs = new ArrayList<Job>();
        /** number of examine jobs */               private static int numExamine = 0;

		DatabaseChangesThread() {
			super("Database");
			start();
		}

        public void run() {
            for (;;) {
                UserInterface userInterface = Main.getUserInterface();
                if (userInterface != null)
                    userInterface.invokeLaterBusyCursor(isChangeJobQueuedOrRunning());
                ServerConnection connection = selectConnection();
                Job job;
                if (connection != null) {
                    job = deserializeJob(connection);
                    if (job == null)
                        continue;
                } else {
                    synchronized (this) {
                        job = waitingJobs.remove(0);
                    }
                    if (job.scheduledToAbort || job.aborted)
                        continue;
                }
                if (job.jobType == Type.EXAMINE) {
                    synchronized (this) {
                        job.started = true;
                        startedJobs.add(job);
                        numExamine++;
                    }
                    //System.out.println("Started Job "+job+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
                    Thread t = new ExamineThread(job);
                    t.start();
                } else { 
                    synchronized (this) {
                        assert numExamine == 0;
                        job.started = true;
                        startedJobs.add(job);
                    }
                    job.run();
                }
            }
        }
        
        private synchronized ServerConnection selectConnection() {
            for (;;) {
                // Search for examine
                if (!waitingJobs.isEmpty()) {
                    Job job = waitingJobs.get(0);
                    if (job.scheduledToAbort || job.aborted || job.jobType == Type.EXAMINE)
                        return null;
                }
                if (numExamine == 0) {
                    if (!waitingJobs.isEmpty())
                        return null;
                    if (threadMode == Mode.SERVER && serverJobManager != null) {
                        for (ServerConnection conn: serverJobManager.serverConnections) {
                            if (conn.reader.bytes != null)
                                return conn;
                        }
                    }
                }
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
        }
        
        private Job deserializeJob(ServerConnection connection) {
            try {
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(connection.reader.bytes));
                Job job = (Job)in.readObject();
                job.connection = connection;
                in.close();
                return job;
            } catch (Throwable e) {
                e.printStackTrace();
                connection.sendFailedJob(null, e);
                return null;
            }
        }

		public void run_()
		{
			for (;;)
			{
				Job job = waitChangeJob();

                job.run();
                if (!BATCHMODE)
                {
                    // turn off busy cursor if no more change jobs
                    synchronized(this) {
                        if (!isChangeJobQueuedOrRunning())
                            Main.getUserInterface().invokeLaterBusyCursor(false);
                    }
                }
			}
		}

 		private synchronized Job waitChangeJob() {
			for (;;)
			{
                if (!waitingJobs.isEmpty())
//				if (numStarted < allJobs.size())
				{
                    Job job = waitingJobs.get(0);
//					Job job = (Job)allJobs.get(numStarted);
                    if (job.scheduledToAbort || job.aborted) {
                        // remove jobs that have been aborted
                        removeJob(job);
                        continue;
                    }
					if (job.jobType == Type.EXAMINE)
					{
						job.started = true;
//                        if (job instanceof InthreadExamineJob) {
//                            // notify thread that it can run.
//                            final InthreadExamineJob ijob = (InthreadExamineJob)job;
//                            boolean started = false;
//                            synchronized(ijob.mutex) {
//                                if (ijob.waiting) {
//                                    ijob.waiting = false;
//                                    ijob.mutex.notify();
//                                    started = true;
//                                }
//                            }
//                            if (started) {
//                                assert(false);
//                                numStarted++;
//                                numExamine++;                                
//                            }
//                            continue;
//                        }
                        waitingJobs.remove(0);
                        startedJobs.add(job);
//                        numStarted++;
                        numExamine++;
                        //System.out.println("Started Job "+job+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
						Thread t = new ExamineThread(job);
						t.start();
						continue;
					}
					// job.jobType == Type.CHANGE || jobType == Type.REDO
					if (numExamine == 0)
					{
						job.started = true;
                        waitingJobs.remove(0);
                        startedJobs.add(job);
//						numStarted++;
						return job;
					}
				}
				try {
                    //System.out.println("Waiting, numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
					wait();
				} catch (InterruptedException e) {}
			}
		}
    
		/** Add job to list of jobs */
		private synchronized void addJob(Job j) {
            if (waitingJobs.isEmpty())
                notify();
            waitingJobs.add(j);
//			if (numStarted == allJobs.size())
//				notify();
//            allJobs.add(j);
            if (!BATCHMODE && j.jobType == Type.CHANGE) {
                Main.getUserInterface().invokeLaterBusyCursor(true);
            }
            if (j.getDisplay()) {
                Main.getUserInterface().wantToRedoJobTree();
            }
		}

//        /**
//         * This method is intentionally not synchronized. We must preserve the order of locking:
//         * databaseChangesThread monitor -> InthreadExamineJob.mutex monitor.
//         * However, the databaseChangesThread monitor needs to be released before the call to
//         * j.mutex.wait(), otherwise we will have deadlock because this thread did not give up
//         * the databaseChangeThread monitor.  This is because notify() on the j.mutex is only
//         * called after both the databaseChangeThread and mutex monitor have been acquired, but in a
//         * separate thread.
//         * @param j
//         * @param wait true to wait for lock if not available now, false to not wait
//         * @return true if lock acquired, false otherwise (if wait is true, this method always returns true)
//         */
//        private boolean addInthreadExamineJob(final InthreadExamineJob j, boolean wait) {
//            synchronized(this) {
//                // check if change job running or queued: if so, can't run immediately
//                if (isChangeJobQueuedOrRunning()) {
//                    if (!wait) return false;
//                    // need to queue because it may not be able to run immediately
//                    addJob(j);
//                    synchronized(j.mutex) {
//                        j.waiting = true;
//                    }
//                } else {
//                    // grant examine lock
//                    allJobs.add(j);
//                    numExamine++;
//                    numStarted++;
//                    //System.out.println("Granted Examine Lock for Job "+j+", numStarted="+numStarted+", numExamine="+numExamine+", allJobs="+allJobs.size());
//                    j.incrementLockCount();
//                    return true;
//                }
//            }
//
//            // we are queued
//            synchronized(j.mutex) {
//                // note that j.waiting *could* have been set false already if Job queue was processed
//                // before this code was processed.
//                if (j.waiting) {
//                    try { j.mutex.wait(); } catch (InterruptedException e) { System.out.println("Interrupted in databaseChangesThread"); }
//                }
//            }
//            j.incrementLockCount();
//            return true;
//        }

		/** Remove job from list of jobs */
		private synchronized void removeJob(Job j) {
            if (j.started) {
                startedJobs.remove(j);
            } else {
                if (!waitingJobs.isEmpty() && waitingJobs.get(0) == j)
                    notify();
                waitingJobs.remove(j);
            }
//			int index = allJobs.indexOf(j);
//			if (index != -1) {
//				allJobs.remove(index);
//				if (index == numStarted)
//					notify();
//				if (index < numStarted) numStarted--;
//			}
            //System.out.println("Removed Job "+j+", index was "+index+", numStarted now="+numStarted+", allJobs="+allJobs.size());
            if (j.getDisplay()) {
                Main.getUserInterface().wantToRedoJobTree();
            }
		}

		private synchronized void endExamine(Job j) {
			numExamine--;
            //System.out.println("EndExamine Job "+j+", numExamine now="+numExamine+", allJobs="+allJobs.size());
			if (numExamine == 0)
				notify();
		}

//        /** Get job running in the specified thread */
//        private synchronized Job getJob(Thread t) {
//            for (Iterator<Job> it = allJobs.iterator(); it.hasNext(); ) {
//                Job j = (Job)it.next();
//                if (j.thread == t) return j;
//            }
//            return null;
//        }

        /** get all jobs iterator */
        public synchronized Iterator<Job> getAllJobs() {
            ArrayList<Job> jobsList = new ArrayList<Job>(startedJobs);
            jobsList.addAll(waitingJobs);
//            List<Job> jobsList = new ArrayList<Job>();
//            for (Iterator<Job> it = allJobs.iterator(); it.hasNext() ;) {
//                jobsList.add(it.next());
//            }
            return jobsList.iterator();
        }

        private synchronized boolean isChangeJobQueuedOrRunning() {
            for (Job j: startedJobs) {
                if (j.finished) continue;
                if (j.jobType == Type.CHANGE) return true;
            }
            for (Job j: waitingJobs) {
                if (j.jobType == Type.CHANGE) return true;
            }
//            Iterator<Job> it;
//            for (it = allJobs.iterator(); it.hasNext(); ) {
//                Job j = (Job)it.next();
//                if (j.finished) continue;               // ignore finished jobs
//                if (j.jobType == Type.CHANGE) return true;
//            }
            return false;
        }
	}

    private static class ExamineThread extends Thread {
        private Job job;
        
        ExamineThread(Job job) {
            super(job.jobName);
            assert job.jobType == Type.EXAMINE;
            this.job = job;
 //           job.thread = this;
        }
        
        public void run() {
            job.run();
        }
    }
    
	/** default execution time in milis */      private static final int MIN_NUM_SECONDS = 60000;
	/** database changes thread */              private static DatabaseChangesThread databaseChangesThread;/* = new DatabaseChangesThread();*/
	/** changing job */                         private static Job changingJob;
    /** server job manager */                   private static ServerJobManager serverJobManager; 
    /** stream for cleint to send Jobs. */      private static DataOutputStream clientOutputStream;
    /** True if preferences are accessible. */  private static boolean preferencesAccessible = true;

    /** delete when done if true */             private boolean deleteWhenDone;
    /** display on job list if true */          private boolean display;
    
    // Job Status
    /** job start time */                       protected long startTime;
    /** job end time */                         protected long endTime;
    /** was job started? */                     private boolean started;
    /** is job finished? */                     private boolean finished;
    /** thread aborted? */                      private boolean aborted;
    /** schedule thread to abort */             private boolean scheduledToAbort;
	/** report execution time regardless MIN_NUM_SECONDS */
												private boolean reportExecution = false;
    /** name of job */                          private String jobName;
    /** tool running the job */                 private Tool tool;
    /** type of job (change or examine) */      private Type jobType;
//    /** priority of job */                      private Priority priority;
//    /** bottom of "up-tree" of cells affected */private Cell upCell;
//    /** top of "down-tree" of cells affected */ private Cell downCell;
//    /** status */                               private String status = null;
    /** progress */                             private String progress = null;
    /** list of saved Highlights */             private transient List<Object> savedHighlights;
    /** saved Highlight offset */               private transient Point2D savedHighlightsOffset;
    /** Fields changed on server side. */       private transient ArrayList<Field> changedFields;
//    /** Thread job will run in (null for new thread) */
//                                                private transient Thread thread;
    /** Connection from which we accepted */    private transient ServerConnection connection = null;
    private static Job clientJob;

    public static void setThreadMode(Mode mode) {
        threadMode = mode;
        databaseChangesThread = new DatabaseChangesThread();
        switch (mode) {
            case FULL_SCREEN:
                break;
            case BATCH:
                BATCHMODE = true;
                break;
            case SERVER:
                serverJobManager = new ServerJobManager(socketPort);
                serverJobManager.start();
                break;
            case CLIENT:
                clientLoop(socketPort);
                // unreachable
                break;
        }
    }
   
    public static Mode getRunMode() { return threadMode; }
    
    public Job() {}
                                                
    /**
	 * Constructor creates a new instance of Job.
	 * @param jobName a string that describes this Job.
	 * @param tool the Tool that originated this Job.
	 * @param jobType the Type of this Job (EXAMINE or CHANGE).
	 * @param upCell the Cell at the bottom of a hierarchical "up cone" of change.
	 * If this and "downCell" are null, the entire database is presumed.
	 * @param downCell the Cell at the top of a hierarchical "down tree" of changes/examinations.
	 * If this and "upCell" are null, the entire database is presumed.
	 * @param priority the priority of this Job.
	 */
    public Job(String jobName, Tool tool, Type jobType, Cell upCell, Cell downCell, Priority priority) {
		if (downCell != null) upCell = null; // downCell not implemented. Lock whole database
		this.jobName = jobName;
		this.tool = tool;
		this.jobType = jobType;
//		this.priority = priority;
//		this.upCell = upCell;
//		this.downCell = downCell;
        this.display = true;
        this.deleteWhenDone = true;
        startTime = endTime = 0;
        started = finished = aborted = scheduledToAbort = false;
//        thread = null;
        savedHighlights = new ArrayList<Object>();
        if (jobType == Job.Type.CHANGE || jobType == Job.Type.UNDO)
            saveHighlights();
	}
	
    /**
     * Start a job. By default displays Job on Job List UI, and
     * delete Job when done.  Jobs that have state the user would like to use
     * after the Job finishes (like DRC, NCC, etc) should call
     * <code>startJob(true, true)</code>.
     */
	public void startJob()
	{
        startJob(!BATCHMODE, true);
    }
	
    /**
     * Start the job by placing it on the JobThread queue.
     * If <code>display</code> is true, display job on Job List UI.
     * If <code>deleteWhenDone</code> is true, Job will be deleted
     * after it is done (frees all data and references it stores/created)
     * @param deleteWhenDone delete when job is done if true, otherwise leave it around
     */
    public void startJob(boolean display, boolean deleteWhenDone)
    {
        this.display = display;
        this.deleteWhenDone = deleteWhenDone;

        if (threadMode == Mode.CLIENT && jobType != Type.EXAMINE) {
            Job tmp = testSerialization();
            if (tmp != null) {
                try {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(); 
                    EObjectOutputStream out = new EObjectOutputStream(byteStream);
                    out.writeObject(this);
                    out.close();
                    byte[] bytes = byteStream.toByteArray();
                    clientOutputStream.writeInt(bytes.length);
                    clientOutputStream.write(bytes);
                    clientOutputStream.flush();
                    clientJob = this;
                } catch (IOException e) {
                    System.out.println("Job " + this + " was not launched in CLIENT mode");
                    e.printStackTrace(System.out);
                }
            }
            return;
        }

        if (NOTHREADING) {
            // turn off threading if needed for debugging
            Main.getUserInterface().setBusyCursor(true);
            run();
            Main.getUserInterface().setBusyCursor(false);
        } else {
            databaseChangesThread.addJob(this);
        }
    }

    /**
     * Method to remember that a field variable of the Job has been changed by the doIt() method.
     * @param variableName the name of the variable that changed.
     */
    protected void fieldVariableChanged(String variableName) {
        try {
            Field fld = getClass().getDeclaredField(variableName);
            fld.setAccessible(true);
            changedFields.add(fld);
        } catch (NoSuchFieldException e) {
            e.printStackTrace(System.out);
        }
    }

    Job testSerialization() {
        byte[] bytes = serialize();
        if (bytes == null) return null;
        Job newJob;
        try {
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(byteInputStream);
            return (Job)in.readObject();
        } catch (Throwable e) {
            System.out.println("Can't deserialize Job " + this);
            e.printStackTrace(System.out);
            return null;
        }
    }
    
    byte[] serialize() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new EObjectOutputStream(byteStream);
            out.writeObject(this);
            out.flush();
            return byteStream.toByteArray();
        } catch (Throwable e) {
            System.out.println("Can't serialize Job " + this);
            e.printStackTrace(System.out);
            return null;
        }
    }
    
    /**
     * Method to access scheduled abort flag in Job and
     * set the flag to abort if scheduled flag is true.
     * This is because setAbort and getScheduledToAbort
     * are protected in Job.
     * @return true if job is scheduled for abort or aborted.
     * and it will report it to std output
     */
//    public boolean checkForAbort()
//    {
//        if (getAborted()) return (true);
//        boolean abort = getScheduledToAbort();
//        if (abort)
//        {
//            setAborted();
//            setReportExecutionFlag(true); // Force reporting
//            System.out.println(jobName +" aborted");
//        }
//        return (abort);
//    }

	//--------------------------ABSTRACT METHODS--------------------------
    
    /** This is the main work method.  This method should
     * perform all needed tasks.
     * @throws JobException TODO
     */
    public abstract boolean doIt() throws JobException;
    
    /**
     * This method executes in the Client side after termination of doIt method.
     * This method should perform all needed termination actions.
     * @param jobException null if doIt terminated normally, otherwise exception thrown by doIt.
     */
    public void terminateIt(Throwable jobException) {
        if (jobException == null)
            terminateOk();
        else
            terminateFail(jobException);
    }
    
    /**
     * This method executes in the Client side after normal termination of doIt method.
     * This method should perform all needed termination actions.
     */
    public void terminateOk() {}
    
    /**
     * This method executes in the Client side after exceptional termination of doIt method.
     * @param jobException null exception thrown by doIt.
     */
    public void terminateFail(Throwable jobException) {
        if (jobException instanceof CantEditException) {
            ((CantEditException)jobException).presentProblem();
        } else if (jobException instanceof JobException) {
            System.out.println(jobException.getMessage());
        }
    }
    
    //--------------------------PRIVATE JOB METHODS--------------------------

//    /** Locks the database to prevent changes to it */
//    private void lockDatabase() {
//
//    }
//
//    /** Unlocks database */
//    private void unlockDatabase() {
//
//    }

	//--------------------------PROTECTED JOB METHODS-----------------------
	/** Set reportExecution flag on/off */
	protected void setReportExecutionFlag(boolean flag)
	{
		reportExecution = flag;
	}
    //--------------------------PUBLIC JOB METHODS--------------------------

    /** Run gets called after the calling thread calls our start method */
    public void run() {
        startTime = System.currentTimeMillis();

        Job serverJob = this;
        String className = getClass().getName();
        if (getDebug() && connection == null && !className.endsWith("RenderJob")) {
            serverJob = testSerialization();
            if (serverJob != null) {
                // transient fields ???
//                serverJob.savedHighlights = this.savedHighlights;
//                serverJob.savedHighlightsOffset = this.savedHighlightsOffset;
            } else {
                serverJob = this;
            }
        }
        serverJob.changedFields = new ArrayList<Field>();
        
        if (DEBUG) System.out.println(jobType+" Job: "+jobName+" started");

        Cell cell = Main.getUserInterface().getCurrentCell();
        if (connection == null)
            ActivityLogger.logJobStarted(jobName, jobType, cell, savedHighlights, savedHighlightsOffset);
        Throwable jobException = null;
		try {
            if (jobType != Type.EXAMINE) changingJob = this;
			if (jobType == Type.CHANGE)	{
                Undo.startChanges(tool, jobName, /*upCell,*/ savedHighlights, savedHighlightsOffset);
                if (!className.endsWith("InitDatabase"))
                    preferencesAccessible = false;
            }
            try {
                serverJob.doIt();
            } catch (JobException e) {
                jobException = e;
            }
			if (jobType == Type.CHANGE)	{
                preferencesAccessible = true;
                Undo.endChanges();
            }
		} catch (Throwable e) {
            jobException = e;
            e.printStackTrace(System.err);
            ActivityLogger.logException(e);
            if (e instanceof Error) throw (Error)e;
		} finally {
			if (jobType == Type.EXAMINE)
			{
				databaseChangesThread.endExamine(this);
			} else {
				changingJob = null;
                if (threadMode == Mode.SERVER)
                    serverJobManager.updateSnapshot();
			}
		}
        if (connection != null) {
            assert threadMode == Mode.SERVER;
            if (jobException != null) {
                connection.sendFailedJob(serverJob, jobException);
            } else {
                try {
                    byte[] bytes = null;
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream out = new EObjectOutputStream(byteStream);
                    out.writeObject(null); // No exception
                    out.writeInt(serverJob.changedFields.size());
                    for (Field f: serverJob.changedFields) {
                        Object value = f.get(serverJob);
                        out.writeUTF(f.getName());
                        out.writeObject(value);
                    }
                    out.close();
                    connection.sendTerminateJob(serverJob, byteStream.toByteArray());
                } catch (Throwable e) {
                    connection.sendFailedJob(serverJob, e);
                }
            }
       } else {
            try {
                if (!serverJob.changedFields.isEmpty()) {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream out = new EObjectOutputStream(byteStream);
                    for (Field f: serverJob.changedFields) {
                        Object value = f.get(serverJob);
                        out.writeObject(value);
                    }
                    out.close();
                    
                    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteStream.toByteArray());
                    ObjectInputStream in = new ObjectInputStream(byteInputStream);
                    for (Field f: serverJob.changedFields) {
                        Object value = in.readObject();
                        f.set(this, value);
                    }
                    in.close();
                }
                
                terminateIt(jobException);

            } catch (Throwable e) {
                ActivityLogger.logException(e);
            }
        }
        endTime = System.currentTimeMillis();
               
        if (DEBUG) System.out.println(jobType+" Job: "+jobName +" finished");

		finished = true;                        // is this redundant with Thread.isAlive()?
//        Job.removeJob(this);
        //WindowFrame.wantToRedoJobTree();

		// say something if it took more than a minute by default
		if (reportExecution || (endTime - startTime) >= MIN_NUM_SECONDS)
		{
			if (User.isBeepAfterLongJobs())
			{
				Toolkit.getDefaultToolkit().beep();
			}
			System.out.println(this.getInfo());
		}

        // delete
        if (deleteWhenDone) {
            databaseChangesThread.removeJob(this);
        }
    }

//    /**
//     * Method to end the current batch of changes and start another.
//     * Besides starting a new batch, it cleans up the constraint system, and 
//     */
//    protected void flushBatch()
//    {
//		if (jobType != Type.CHANGE)	return;
//		Undo.endChanges();
//		Undo.startChanges(tool, jobName, /*upCell,*/ savedHighlights, savedHighlightsOffset);
//    }

    protected synchronized void setProgress(String progress) {
        this.progress = progress;
        Main.getUserInterface().wantToRedoJobTree();
    }        
    
    private synchronized String getProgress() { return progress; }

    /** Return run status */
    public boolean isFinished() { return finished; }
    
    /** Tell thread to abort. Extending class should check
     * abort when/where applicable
     */
    public synchronized void abort() {
        if (aborted) { 
            System.out.println("Job already aborted: "+getStatus());
            return;
        }
        scheduledToAbort = true;
        Main.getUserInterface().wantToRedoJobTree();
    }

    /** Save current Highlights */
    private void saveHighlights() {
        savedHighlights.clear();

        // for now, just save highlights in current window
        UserInterface ui = Main.getUserInterface();
        EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

        savedHighlights = wnd.saveHighlightList();
        savedHighlightsOffset = wnd.getHighlightOffset();
    }

	/** Confirmation that thread is aborted */
    protected synchronized void setAborted() { aborted = true; Main.getUserInterface().wantToRedoJobTree(); }
    /** get scheduled to abort status */
    protected synchronized boolean getScheduledToAbort() { return scheduledToAbort; }
    /**
     * Method to get abort status. Please leave this function as private.
     * To retrieve abort status from another class, use checkAbort which also
     * checks if job is scheduled to be aborted.
     * @return
     */
    private synchronized boolean getAborted() { return aborted; }
    /** get display status */
    public boolean getDisplay() { return display; }
    /** get deleteWhenDone status */
    public boolean getDeleteWhenDone() { return deleteWhenDone; }

	/**
	* Check if we are scheduled to abort. If so, print msg if non null
	 * and return true.
     * This is because setAbort and getScheduledToAbort
     * are protected in Job.
	 * @return true on abort, false otherwise. If job is scheduled for abort or aborted.
     * and it will report it to std output
	 */
	public boolean checkAbort()
	{
		if (getAborted()) return (true);
		boolean scheduledAbort = getScheduledToAbort();
		if (scheduledAbort)
		{
			System.out.println(this + ": aborted");  // should call Job.toString()
            setReportExecutionFlag(true); // Force reporting
			setAborted();                   // Job has been aborted
		}
		return scheduledAbort;
	}

    /** get all jobs iterator */
    public static Iterator<Job> getAllJobs() { return databaseChangesThread.getAllJobs(); }
    
    /** get status */
    public String getStatus() {
		if (!started) return "waiting";
        if (aborted) return "aborted";
        if (finished) return "done";
        if (scheduledToAbort) return "scheduled to abort";
        if (getProgress() == null) return "running";
        return getProgress();
    }

    /** Remove job from Job list if it is done */
    public boolean remove() {
        if (!finished && !aborted) {
            //System.out.println("Cannot delete running jobs.  Wait till finished or abort");
            return false;
        }
        databaseChangesThread.removeJob(this);
        return true;
    }

//    /**
//     * This Job serves as a locking mechanism for acquireExamineLock()
//     */
//    private static class InthreadExamineJob extends Job {
//        private boolean waiting;
//        private int lockCount;
//        protected final Object mutex = new Object();
//
//        private InthreadExamineJob() {
//            super("Inthread Examine", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
//            waiting = false;
//            lockCount = 0;
//        }
//        public boolean doIt() throws JobException {
//            // this should never be called
//            assert(false);
//            return true;
//        }
//        private void incrementLockCount() { lockCount++; }
//        private void decrementLockCount() { lockCount--; }
//        private int getLockCount() { return lockCount; }
//    }

    /**
     * Unless you need to code to execute quickly (such as in the GUI thread)
     * you should be using a Job to examine the database instead of this method.
     * The suggested format is as follows:
     * <p>
     * <pre><code>
     * if (Job.acquireExamineLock(block)) {
     *     try {
     *         // do stuff
     *         Job.releaseExamineLock();    // release lock
     *     } catch (Error e) {
     *         Job.releaseExamineLock();    // release lock if error/exception thrown
     *         throw e;                     // rethrow error/exception
     *     }
     * }
     * </code></pre>
     * <p>
     * This method tries to acquire a lock to allow the current thread to
     * safely examine the database.
     * If "block" is true, this call blocks until a lock is acquired.  If block
     * is false, this call returns immediately, returning true if a lock was
     * acquired, or false if not.  You must call Job.releaseExamineLock
     * when done if you acquired a lock.
     * <p>
     * Subsequent nested calls to this method from the same thread must
     * have matching calls to releaseExamineLock.
     * @param block True to block (wait) until lock can be acquired. False to
     * return immediately with a return value that denotes if a lock was acquired.
     * @return true if lock acquired, false otherwise. If block is true,
     * the return value is always true.
     * @see #releaseExamineLock()
     * @see #invokeExamineLater(Runnable, Object)
     */
    public static synchronized boolean acquireExamineLock(boolean block) {
        return true;
//        if (true) return true;      // disabled
//        Thread thread = Thread.currentThread();
//
//        // first check to see if we already have the lock
//        Job job = databaseChangesThread.getJob(thread);
//        if (job != null) {
//            assert(job instanceof InthreadExamineJob);
//            ((InthreadExamineJob)job).incrementLockCount();
//            return true;
//        }
//        // create new Job to get examine lock
//        InthreadExamineJob dummy = new InthreadExamineJob();
//        ((Job)dummy).display = false;
//        ((Job)dummy).deleteWhenDone = true;
//        ((Job)dummy).thread = thread;
//        return databaseChangesThread.addInthreadExamineJob(dummy, block);
    }

    /**
     * Release the lock to examine the database.  The lock is the lock
     * associated with the current thread.  This should only be called if a
     * lock was acquired for the current thread.
     * @see #acquireExamineLock(boolean)
     * @see #invokeExamineLater(Runnable, Object)
     */
    public static synchronized void releaseExamineLock() {
//        if (true) return;      // disabled
//        Job dummy = databaseChangesThread.getJob(Thread.currentThread());
//        assert(dummy != null);
//        assert(dummy instanceof InthreadExamineJob);
//        InthreadExamineJob job = (InthreadExamineJob)dummy;
//        job.decrementLockCount();
//        if (job.getLockCount() == 0) {
//            databaseChangesThread.endExamine(job);
//            databaseChangesThread.removeJob(job);
//        }
    }

//    /**
//     * See if the current thread already has an Examine Lock.
//     * This is useful when you want to assert that the running
//     * thread has successfully acquired an Examine lock.
//     * This methods returns true if the current thread is a Job
//     * thread, or if the current thread has successfully called
//     * acquireExamineLock.
//     * @return true if the current thread has an active examine
//     * lock on the database, false otherwise.
//     */
//    public static synchronized boolean hasExamineLock() {
//        Thread thread = Thread.currentThread();
//        // change job is a valid examine job
//        if (thread == databaseChangesThread) return true;
//        // check for any examine jobs
//        Job job = databaseChangesThread.getJob(thread);
//        if (job != null) {
//            return true;
//        }
//        return false;
//    }

    /**
     * A common pattern is that the GUI needs to examine the database, but does not
     * want to wait if it cannot immediately get an Examine lock via acquireExamineLock.
     * In this case the GUI can call invokeExamineLater to have the specified SwingExamineTask
     * run in the context of the swing thread where it will be *guaranteed* that it will
     * be able to acquire an Examine Lock via acquireExamineLock().
     * <p>
     * This method basically reserves a slot in the Job queue with an Examine Job,
     * calls the runnable with SwingUtilities.invokeAndWait when the Job starts, and
     * ends the Job only after the runnable finishes.
     * <P>
     * IMPORTANT!  Note that this ties up both the Job queue and the Swing event queue.
     * It is possible to deadlock if the SwingExamineJob waits on a Change Job thread (unlikely,
     * but possible).  Note that this also runs examines sequentially, because that is
     * how the Swing event queue runs events.  This is less efficient than the Job queue examines,
     * but also maintains sequential ordering and process of events, which may be necessary
     * if state is being shared/modified between events (such as between mousePressed and
     * mouseReleased events).
     * @param task the Runnable to run in the swing thread. A call to
     * Job.acquireExamineLock from within run() is guaranteed to return true.
     * @param singularKey if not null, this specifies a key by which
     * subsequent calls to this method using the same key will be consolidated into
     * one invocation instead of many.  Only calls that have not already resulted in a
     * call back to the runnable will be ignored.  Only the last runnable will be used.
     * @see SwingExamineTask  for a common pattern using this method
     * @see #acquireExamineLock(boolean)
     * @see #releaseExamineLock()
     */
    public static void invokeExamineLater(Runnable task, Object singularKey) {
        assert false;
//        if (singularKey != null) {
//            SwingExamineJob priorJob = SwingExamineJob.getWaitingJobFor(singularKey);
//            if (priorJob != null)
//                priorJob.abort();
//        }
//        SwingExamineJob job = new SwingExamineJob(task, singularKey);
//        job.startJob(false, true);
    }

//    private static class SwingExamineJob extends Job {
//        /** Map of Runnables to waiting Jobs */         private static final Map<Object,Job> waitingJobs = new HashMap<Object,Job>();
//        /** The runnable to run in the Swing thread */  private Runnable task;
//        /** the singular key */                         private Object singularKey;
//
//        private SwingExamineJob(Runnable task, Object singularKey) {
//            super("ReserveExamineSlot", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
//            this.task = task;
//            this.singularKey = singularKey;
//            synchronized(waitingJobs) {
//                if (singularKey != null)
//                    waitingJobs.put(singularKey, this);
//            }
//        }
//
//        public boolean doIt() throws JobException {
//            synchronized(waitingJobs) {
//                if (singularKey != null)
//                    waitingJobs.remove(singularKey);
//            }
//            try {
//                SwingUtilities.invokeAndWait(task);
//            } catch (InterruptedException e) {
//            } catch (java.lang.reflect.InvocationTargetException ee) {
//            }
//            return true;
//        }
//
//        private static SwingExamineJob getWaitingJobFor(Object singularKey) {
//            if (singularKey == null) return null;
//            synchronized(waitingJobs) {
//                return (SwingExamineJob)waitingJobs.get(singularKey);
//            }
//        }
//    }

	/**
	 * Method to check whether examining of database is allowed.
	 */
    public static void checkExamine() {
        if (!getDebug()) return;
        if (NOTHREADING) return;
	    /*
	    // disabled by Gilda on Oct 18
        if (!hasExamineLock()) {
            String msg = "Database is being examined without an Examine Job or Examine Lock";
            System.out.println(msg);
            Error e = new Error(msg);
            throw e;
        }
        */
    }

//	/**
//	 * Returns thread which activated current changes or null if no changes.
//	 * @return thread which activated current changes or null if no changes.
//	 */
//	public static Thread getChangingThread() { return changingJob != null ? databaseChangesThread : null; }
//
//    /**
//     * Returns the current Changing job (there can be only one)
//     * @return the current Changing job (there can be only one). Returns null if no changing job.
//     */
//    public static Job getChangingJob() { return changingJob; }
//
//
//	/**
//	 * Returns cell which is root of up-tree of current changes or null, if no changes or whole database changes.
//	 * @return cell which is root of up-tree of current changes or null, if no changes or whole database changes.
//	 */
//	public static Cell getChangingCell() { return changingJob != null ? changingJob.upCell : null; }

	/**
	 * Method to check whether changing of whole database is allowed.
	 * Issues an error if it is not.
	 */
	public static void checkChanging()
	{
		if (Thread.currentThread() != databaseChangesThread)
		{
			if (NOTHREADING) return;
            if (threadMode == Mode.CLIENT) return;
			String msg = "Database is being changed by another thread";
            System.out.println(msg);
			throw new IllegalStateException(msg);
		} else if (changingJob == null)
		{
			if (NOTHREADING) return;
			String msg = "Database is changing but no change job is running";
            System.out.println(msg);
			throw new IllegalStateException(msg);
		}
/*       else if (changingJob.upCell != null)
		{
			String msg = "Database is changing which only up-tree of "+changingJob.upCell+" is locked";
            System.out.println(msg);
            Error e = new Error(msg);
            throw e;
			//throw new IllegalStateException("Undo.checkChanging()");
		}*/
	}

    /**
     * Checks if bounds or netlist can be computed in this thread.
     * @return true if bounds or netlist can be computed.
     */
    public static boolean canCompute() {
        return Thread.currentThread() == databaseChangesThread || Job.NOTHREADING || threadMode == Mode.CLIENT;
    }
    
    /**
     * Asserts that this is the Swing Event thread
     */
    public static void checkSwingThread()
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            Exception e = new Exception("Job.checkSwingThread is not in the AWT Event Thread, it is in Thread "+Thread.currentThread());
            ActivityLogger.logException(e);
        }
    }

    public static boolean preferencesAccessible()
    {
        return preferencesAccessible || Thread.currentThread() != databaseChangesThread;
    }
    
	//-------------------------------JOB UI--------------------------------
    
    public String toString() { return jobName+" ("+getStatus()+")"; }


//    /** respond to menu item command */
//    public void actionPerformed(ActionEvent e) {
//        JMenuItem source = (JMenuItem)e.getSource();
//        // extract library and cell from string
//        if (source.getText().equals("Get Info"))
//            System.out.println(getInfo());
//        if (source.getText().equals("Abort"))
//            abort();
//        if (source.getText().equals("Delete")) {
//            if (!finished && !aborted) {
//                System.out.println("Cannot delete running jobs.  Wait till it is finished, or abort it");
//                return;
//            }
//            databaseChangesThread.removeJob(this);
//        }
//    }

    /** Get info on Job */
    public String getInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append("Job "+toString());
        Date start = new Date(startTime);
        //buf.append("  start time: "+start+"\n");
        if (finished) {
//            Date end = new Date(endTime);
            //buf.append("  end time: "+end+"\n");
            long time = endTime - startTime;
            buf.append(" took: "+TextUtils.getElapsedTime(time));
            buf.append(" (started at "+start+")");
        } else if (getProgress() == null) {
            long time = System.currentTimeMillis()-startTime;
	        buf.append(" has not finished. Current running time: " + TextUtils.getElapsedTime(time));
        } else {
            buf.append(" did not successfully finish.");
        }
        return buf.toString();
    }
        
    private static class ServerJobManager extends Thread {
        private final int port;
        private final ArrayList<ServerConnection> serverConnections = new ArrayList<ServerConnection>();
        private int cursor;
        private volatile Snapshot currentSnapshot = new Snapshot();
        
        ServerJobManager(int port) {
            super(null, null, "ConnectionWaiter", 0/*10*1020*/);
            this.port = port;
        }
        
        public void updateSnapshot() {
            Snapshot oldSnapshot = currentSnapshot;
            Snapshot newSnapshot = new Snapshot(oldSnapshot);
            if (newSnapshot.equals(oldSnapshot)) return;
            synchronized (this) {
                currentSnapshot = newSnapshot;
                for (ServerConnection conn: serverConnections)
                    conn.updateSnapshot(newSnapshot);
            }
        }
        
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(port);
                System.out.println("ServerSocket waits for port " + port);
                for (;;) {
                    Socket socket = ss.accept();
                    ServerConnection conn;
                    synchronized (this) {
                        conn = new ServerConnection(serverConnections.size(), socket);
                        serverConnections.add(conn);
                    }
                    System.out.println("Accepted connection " + conn.connectionId);
                    conn.updateSnapshot(currentSnapshot);
                    conn.start();
                    conn.reader.start();
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        }
    }
    
    private static class ServerConnection extends Thread {
        private static final int STACK_SIZE = 20*1024;
        private final int connectionId;
        private final Socket socket;
        private final ConnectionReader reader;
        private Snapshot currentSnapshot;
        private volatile Snapshot newSnapshot;
        private volatile String jobName;
        private volatile byte[] result;
        
        ServerConnection(int connectionId, Socket socket) {
            super(null, null, "ServerConnection-" + connectionId, STACK_SIZE);
            this.connectionId = connectionId;
            this.socket = socket;
            newSnapshot = currentSnapshot = new Snapshot();
            reader = new ConnectionReader("ConndectionReader-" + connectionId, socket);
        }
        
        synchronized void updateSnapshot(Snapshot newSnapshot) {
            if (this.result != null) return;
            this.newSnapshot = newSnapshot;
            notify();
        }
        
        synchronized void sendTerminateJob(Job job, byte[] result) {
            assert this.result == null;
            assert result != null;
            this.result = result;
            jobName = job != null ? job.jobName : "Unknown";
            notify();
        }  
        
        void sendFailedJob(Job job, Throwable e) {
            byte[] bytes = null;
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream out = new EObjectOutputStream(byteStream);
                out.writeObject(e);
                out.writeInt(0);
                out.close();
                bytes = byteStream.toByteArray();
            } catch (Throwable ee) {}
            sendTerminateJob(job, bytes);
        }
        
       public void run() {
            try {
                SnapshotWriter writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())));
                for (;;) {
                    Snapshot newSnapshot;
                    synchronized (this) {
                        while (this.newSnapshot == currentSnapshot && result == null)
                            wait();
                        newSnapshot = this.newSnapshot;
                    }
                    if (newSnapshot != currentSnapshot) {
                        writer.out.writeByte(1);
                        newSnapshot.writeDiffs(writer, currentSnapshot);
                        writer.out.flush();
                        currentSnapshot = newSnapshot;
                    }
                    if (result != null) {
                        writer.out.writeByte(2);
                        writer.out.writeUTF(jobName);
                        writer.out.writeInt(result.length);
                        writer.out.write(result);
                        writer.out.flush();
                        result = null;
                        reader.enable();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.out);
            } catch (InterruptedException e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private static class ConnectionReader extends Thread {
        private final static int STACK_SIZE = 1024;
        private final Socket socket;
        private byte[] bytes;
        
        ConnectionReader(String name, Socket socket) {
            super(null, null, name, STACK_SIZE);
            this.socket = socket;
        }
        
        byte[] getBytes() {
            if (bytes == null) return null;
            byte[] bytes = this.bytes;
            notify();
            return bytes;
        }
        
        synchronized void enable() {
            assert this.bytes != null;
            this.bytes = null;
            notify();
        }
        
        public void run() {
            try {
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                for (;;) {
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    this.bytes = bytes;
                    synchronized (databaseChangesThread) {
                        databaseChangesThread.notify();
                    }
                    synchronized (this) {
                        while (this.bytes != null) {
                            try {
                                wait();
                            } catch (InterruptedException e) {}
                        }
                    }
//                    try {
//                        ObjectInputStream ino = new ObjectInputStream(new ByteArrayInputStream(bytes));
//                        Job job = (Job)ino.readObject();
//                        ino.close();
//                        job.fromNetwork = true;
//                        databaseChangesThread.addJob(job);
//                    } catch (Throwable e) {
//                        System.out.println("Job deserialization failed");
//                        e.printStackTrace(System.out);
//                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void clientLoop(int port) {
        SnapshotReader reader = null;
        Snapshot currentSnapshot = new Snapshot();
        try {
            System.out.println("Attempting to connect to port " + port + " ...");
            Socket socket = new Socket((String)null, port);
            reader = new SnapshotReader(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
            clientOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println("Connected");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
            
        Technology.initAllTechnologies();
        User.getUserTool().init();
        NetworkTool.getNetworkTool().init();
        //Tool.initAllTools();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // remove the splash screen
                //if (sw != null) sw.removeNotify();
                TopLevel.InitializeWindows();
                WindowFrame.wantToOpenCurrentLibrary(true);
            }
        });
        
        for (;;) {
            try {
                int tag = reader.in.readByte();
                switch (tag) {
                    case 1:
                        Snapshot newSnapshot = Snapshot.readSnapshot(reader, currentSnapshot);
                        SwingUtilities.invokeLater(new SnapshotDatabaseChangeRun(currentSnapshot, newSnapshot));
                        currentSnapshot = newSnapshot;
                        break;
                    case 2:
                        String jobName = reader.in.readUTF();
                        int len = reader.in.readInt();
                        byte[] bytes = new byte[len];
                        reader.in.readFully(bytes);
                        System.out.println("Result of Job <" + jobName + "> is packed in " + len + " bytes");
                        try {
                            assert clientJob.jobName.equals(jobName);
                            Class jobClass = clientJob.getClass();
                            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                            Throwable jobException = (Throwable)in.readObject();
                            System.out.println("\tjobException = " + jobException);
                            int numFields = in.readInt();
                            for (int i = 0; i < numFields; i++) {
                                String fieldName = in.readUTF();
                                Object value = in.readObject();
                                System.out.println("\tField " + fieldName + " = " + value);
                                Field f = jobClass.getDeclaredField(fieldName);
                                f.setAccessible(true);
                                f.set(clientJob, value);
                            }
                            in.close();
                            clientJob.terminateIt(jobException);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        assert false;
                }
            } catch (IOException e) {
                // reader.in.close();
                reader = null;
                System.out.println("END OF FILE");
                return;
            }
        }
    }
    
    private static class SnapshotDatabaseChangeRun implements Runnable 
	{
		private Snapshot oldSnapshot;
        private Snapshot newSnapshot;
		private SnapshotDatabaseChangeRun(Snapshot oldSnapshot, Snapshot newSnapshot) {
            this.oldSnapshot = oldSnapshot;
            this.newSnapshot = newSnapshot;
        }
        public void run() {
            boolean cellTreeChanged = Library.updateAll(oldSnapshot, newSnapshot);
            NetworkTool.updateAll(oldSnapshot, newSnapshot);
            for (int i = 0; i < newSnapshot.cellBackups.size(); i++) {
            	CellBackup newBackup = newSnapshot.getCell(i);
            	CellBackup oldBackup = oldSnapshot.getCell(i);
                ERectangle newBounds = newSnapshot.getCellBounds(i);
                ERectangle oldBounds = oldSnapshot.getCellBounds(i);
            	if (newBackup != oldBackup || newBounds != oldBounds) {
            		Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
            		User.markCellForRedraw(cell, true);
            	}
            }
            SnapshotDatabaseChangeEvent event = new SnapshotDatabaseChangeEvent(cellTreeChanged);
            Undo.fireDatabaseChangeEvent(event);
        }
	}
    
    private static class SnapshotDatabaseChangeEvent extends DatabaseChangeEvent {
        private boolean cellTreeChanged;
        
        SnapshotDatabaseChangeEvent(boolean cellTreeChanged) {
            super(null);
            this.cellTreeChanged = cellTreeChanged;
        }
        
        /**
         * Returns true if ElectricObject eObj was created, killed or modified
         * in the new database state.
         * @param eObj ElectricObject to test.
         * @return true if the ElectricObject was changed.
         */
        public boolean objectChanged(ElectricObject eObj) { return true; }
        
        /**
         * Returns true if cell explorer tree was changed
         * in the new database state.
         * @return true if cell explorer tree was changed.
         */
        public boolean cellTreeChanged() {
            return cellTreeChanged;
//                if (change.getType() == Undo.Type.VARIABLESMOD && change.getObject() instanceof Cell) {
//                    ImmutableElectricObject oldImmutable = (ImmutableElectricObject)change.getO1();
//                    ImmutableElectricObject newImmutable = (ImmutableElectricObject)change.getObject().getImmutable();
//                    return oldImmutable.getVar(Cell.MULTIPAGE_COUNT_KEY) != newImmutable.getVar(Cell.MULTIPAGE_COUNT_KEY);
//                }
        }
    }
    
}
