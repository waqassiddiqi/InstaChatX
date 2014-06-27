package com.appsrox.instachat.model;

/***
 * Describes a Contact
 * @author waqas.siddiqui
 *
 */
public class Contact {
	private String email;
	private String displayName;
	
	public Contact() { }
	
	public Contact(String email, String displayName) {
		this.email = email;
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
