package com.example.lcom75.multiimageselect.customviews;

public interface RequestDelegate {
    void run(TLObject response, TLRPC.TL_error error);
}
