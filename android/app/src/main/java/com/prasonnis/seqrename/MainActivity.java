package com.prasonnis.seqrename;

import android.content.Intent;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(SeqRenamePlugin.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SeqRenamePlugin.REQUEST_CODE_WRITE) {
            Plugin plugin = getBridge().getPlugin("SeqRename").getInstance();
            if (plugin instanceof SeqRenamePlugin) {
                ((SeqRenamePlugin) plugin).handleWriteRequestResult(resultCode);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
