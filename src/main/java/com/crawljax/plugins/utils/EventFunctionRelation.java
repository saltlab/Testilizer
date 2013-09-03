package com.crawljax.plugins.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;

/**
 * EventFunctionRelation is used to store which JavaScript functions are called after firing a clickable event
 * 
 * @author Amin Milani Fard
 */
public class EventFunctionRelation implements Serializable{

	private static final long serialVersionUID = 6310814210074770362L;

	private Eventable event;
	private StateVertex stateBefore;
	private StateVertex stateAfter;
	private ArrayList<String> javaScriptFunctions = new ArrayList<String>();	// keeping name of executed JavaScript functions

	
	public EventFunctionRelation(Eventable event, StateVertex stateBefore,
			StateVertex stateAfter, ArrayList<String> executedJSFunctions) {
		super();
		this.event = event;
		this.stateBefore = stateBefore;
		this.stateAfter = stateAfter;
		this.javaScriptFunctions = executedJSFunctions;
	}


	@Override
	public String toString() {
		return "EventFunctionRelation [event=" + event + ", stateBefore="
				+ stateBefore + ", stateAfter=" + stateAfter
				+ ", javaScriptFunctions=" + javaScriptFunctions + "]";
	}




}
