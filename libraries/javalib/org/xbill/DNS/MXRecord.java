// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * Mail Exchange - specifies where mail to a domain is sent
 *
 * @author Brian Wellington
 */

public class MXRecord extends U16NameBase {

MXRecord() {}

Record
getObject() {
	return new MXRecord();
}

/**
 * Creates an MX Record from the given data
 * @param priority The priority of this MX.  Records with lower priority
 * are preferred.
 * @param target The host that mail is sent to
 */
public
MXRecord(Name name, int dclass, long ttl, int priority, Name target) {
	super(name, Type.MX, dclass, ttl, priority, "priority",
	      target, "target");
}

/** Returns the target of the MX record */
public Name
getTarget() {
	return getNameField();
}

/** Returns the priority of this MX record */
public int
getPriority() {
	return getU16Field();
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU16(u16Field);
	nameField.toWire(out, c, canonical);
}

public Name
getAdditionalName() {
	return getNameField();
}

}