/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PoolWorkerStrategy.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.util.concurrent.runtime;

/**
 * 
 * Strategy pattern for pool worker
 * 
 * @author Felix Schmidt
 * 
 */
public abstract class WorkerStrategy {

    protected volatile int executed = 0;
    protected int threadId = -1;
    protected volatile boolean abort;

    /**
     * Abstract method, this method should contain the body of the worker
     * strategy
     */
    public abstract void execute();

    /**
     * shutdown the current worker
     */
    public void shutdown() {
        abort = true;
    }

    public int getExecutedCounter() {
        return this.executed;
    }

    public int getThreadID() {
        return this.threadId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Thread " + getThreadID() + ": " + getExecutedCounter();
    }

}
