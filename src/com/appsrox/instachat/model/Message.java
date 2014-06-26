/*
 * 
 */
package com.appsrox.instachat.model;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Describes a chat message containing information about sender, receiver
 * attachment and time to self destroy.
 *
 * @author Waqas Hussain Siddiqui (waqas.siddiqi@hotmail.com)
 * @version 0.1
 */
public class Message {
	
	/* type of message  */
	/** The Constant UNICAST. */
	public static final int UNICAST = 1;
	
	/** The Constant MULTICAST. */
	public static final int MULTICAST = 2;
	
	/** The Constant BROADCAST. */
	public static final int BROADCAST = 3;
	
	/* message category  */
	/** The Constant ACTION_IM. */
	public static final int ACTION_IM = 1;
	
	/** The Constant ACTION_DELETE. */
	public static final int ACTION_DELETE = 2;
	
	/* unique id of message  */
	/** The uuid. */
	private String uuid;
	
	/* type of message  */
	/** The type. */
	private int type;
	
	/* list of recepient, for UNICAT it can only have one value  */
	/** The recepient. */
	private List<String> recepient;
	
	/* message to display  */
	/** The message. */
	private String message;
	
	/* time in seconds after which message will be self destroyed  */
	/** The ttl. */
	private int ttl;
	
	/* detail of sender  */
	/** The sender. */
	private String sender;
	
	/* message category  */
	/** The action. */
	private int action;
	
	/* url to associated attachment  */
	/** The attachment. */
	private String attachment;
	
	
	/* ************ Getter and Setter Methods ************ */
	
	/**
	 * Gets the attachment.
	 *
	 * @return the attachment
	 */
	public String getAttachment() {
		return attachment;
	}
	
	/**
	 * Sets the attachment.
	 *
	 * @param attachment the new attachment
	 */
	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
	
	/**
	 * Gets the action.
	 *
	 * @return the action
	 */
	public int getAction() {
		return action;
	}
	
	/**
	 * Sets the action.
	 *
	 * @param action the new action
	 */
	public void setAction(int action) {
		this.action = action;
	}

	/**
	 * Gets the sender.
	 *
	 * @return the sender
	 */
	public String getSender() {
		return sender;
	}
	
	/**
	 * Sets the sender.
	 *
	 * @param sender the new sender
	 */
	public void setSender(String sender) {
		this.sender = sender;
	}

	/**
	 * Instantiates a new message.
	 */
	public Message() {
		recepient = new ArrayList<String>();
	}
	
	/**
	 * Gets the uuid.
	 *
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * Sets the uuid.
	 *
	 * @param uuid the new uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Sets the type.
	 *
	 * @param type the new type
	 */
	public void setType(int type) {
		this.type = type;
	}
	
	/**
	 * Gets the recepient.
	 *
	 * @return the recepient
	 */
	public List<String> getRecepient() {
		return recepient;
	}
	
	/**
	 * Sets the recepient.
	 *
	 * @param recepient the new recepient
	 */
	public void setRecepient(List<String> recepient) {
		this.recepient = recepient;
	}
	
	/**
	 * Gets the message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Sets the message.
	 *
	 * @param message the new message
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
	/**
	 * Gets the ttl.
	 *
	 * @return the ttl
	 */
	public int getTtl() {
		return ttl;
	}
	
	/**
	 * Sets the ttl.
	 *
	 * @param ttl the new ttl
	 */
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
}
