package com.vmware.http.request;

public enum RequestBodyHandling {
    AsUrlEncodedJsonEntity,
    AsStringJsonEntity,
    AsUrlEncodedFormEntity,
    AsMultiPartFormEntity
}
