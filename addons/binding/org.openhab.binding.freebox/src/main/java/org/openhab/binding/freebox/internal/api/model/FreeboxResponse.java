/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.freebox.internal.api.model;

import org.openhab.binding.freebox.internal.api.FreeboxException;

/**
 * The {@link FreeboxResponse} is the Java class used to map the "APIResponse"
 * structure used by the API
 * https://dev.freebox.fr/sdk/os/#
 *
 * @author Laurent Garnier - Initial contribution
 */
public class FreeboxResponse<T> {
    private static final String AUTHORIZATION_REQUIRED = "auth_required";
    private static final String INSUFFICIENT_RIGHTS = "insufficient_rights";

    private Boolean success;
    private String errorCode;
    private String uid;
    private String msg;
    private String missingRight;
    private T result;

    public void evaluate() throws FreeboxException {
        if (!isSuccess()) {
            throw new FreeboxException(this);
        }
    }

    public boolean isAuthRequired() {
        return AUTHORIZATION_REQUIRED.equalsIgnoreCase(errorCode);
    }

    public boolean isMissingRights() {
        return INSUFFICIENT_RIGHTS.equalsIgnoreCase(errorCode);
    }

    public Boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUid() {
        return uid;
    }

    public String getMsg() {
        return msg;
    }

    public String getMissingRight() {
        return missingRight;
    }

    public T getResult() {
        return result;
    }

}
