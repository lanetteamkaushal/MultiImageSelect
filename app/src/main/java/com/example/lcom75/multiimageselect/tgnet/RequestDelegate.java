package com.example.lcom75.multiimageselect.tgnet;

public interface RequestDelegate {
    void run(TLObject response, TLRPC.TL_error error);
}
