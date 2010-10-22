/*
 * Created on Feb 17, 2005
 */
package com.sun.electric.tool.simulation.eventsim.core.engine;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Event;



/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * @author ib27688
 * 
 */
public abstract class EventQueue {

	public abstract int capacity();
	
	public abstract Event nextEvent();
	
	public abstract Event peek();
	
	public abstract void insertEvent(Event newEvent);
	
	public abstract int size();
	
	public abstract boolean isEmpty();
	
	public abstract void clear();
	
	public abstract void print();
	
} // interface Queue
