package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.test;

import java.util.LinkedList;
import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;

public class InputTerminalTestDestination extends ComponentWorker {

	static enum State {WAIT_DATA, WAIT_ACK};
	State state= State.WAIT_DATA;
	
	// the output terminal under test
	InputTerminal inTerm;

	// storage for data received
	LinkedList<Integer> dataReceived= new LinkedList<Integer>();
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	// delay between receiving data and producting acknowledgment
	Delay cycleDelay= new Delay(0);
	// get a new random generator
	Random rndGen= new Random();	
	
	// inform the data source (input terminal) that the data has been received
	Command ackInputCmd= new AckInputCommand();
	// input is here
	Command inputAvailableCmd= new InputAvailableCommand();
	
	public InputTerminalTestDestination(String n) {
		super(n);
	}

	public InputTerminalTestDestination(String name, CompositeEntity g) {
		super(name, g);
	}

	@Override
	public void init() {
		state= State.WAIT_DATA;
		dataReceived.clear();
	}

	@Override
	public boolean selfCheck() {
		boolean check= true;
		return check;
	}

	// the same as attach
	public void setTerminal(InputTerminal t) {
		inTerm= t;
		attachInput(inTerm, inputAvailableCmd);
	} // setTerminal
	
	public void setCycleDelay(Delay d) {
		cycleDelay= d;
	}
	
	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}
	
	Delay calculateDelay(Delay defDelay) {
		Delay d= defDelay;
		if (delayVariation > 0) {
			// get the randomized delay variation
			int delayVar= rndGen.nextInt(delayVariationRange) - delayVariation;
			if (delayVar < 0) delayVar= 0;
			// add the variation to the delay
			d= defDelay.addDelay(delayVar);
		}
		return d;
	}
	
	class AckInputCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_ACK) {
				// about to produce an input ack, start waiting for the next data item
				state= State.WAIT_DATA;
				inTerm.ackInput();
			}
			else {
				fatalError("AckInput command executed in state " + state);
			}
		} // execute
	} // class AckInputCommand
	
	class InputAvailableCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_DATA) {
			    // assume only integers are sent for test purposes
				// record data received
				dataReceived.addLast((Integer)inTerm.getData());
				// wait to produce acknowledgment
				state= State.WAIT_ACK;
				// schedule acknowledgment
				ackInputCmd.trigger(null, calculateDelay(cycleDelay));
			}
			else {
				fatalError("Input arrived before the previous input was acknowledged.");
			}
		} // execute
	} // class InputAvailableCommand
	
} // class InputTerminalTestDestination
