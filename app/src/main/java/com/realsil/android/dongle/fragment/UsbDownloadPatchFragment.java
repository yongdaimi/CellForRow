package com.realsil.android.dongle.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realsil.android.dongle.PathDefine;
import com.realsil.android.dongle.R;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.base.BaseFragment;
import com.realsil.android.dongle.entity.UsbMsg;
import com.realsil.android.dongle.helper.DownloadPatchHelper;
import com.realsil.android.dongle.util.FileUtil;
import com.realsil.android.dongle.util.TimeUtil;
import com.realsil.sdk.core.usb.UsbGatt;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.RtkBTChipVersionInfo;
import com.realsil.sdk.core.usb.connector.UsbError;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadLocalChipVersionInfoRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadRomVersionCommandCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.QueryBTConnectStateRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.ReadLocalChipVersionInfoRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.ReadRomVersionCommand;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class UsbDownloadPatchFragment extends BaseFragment implements View.OnClickListener {


    private Button btn_connect_usb_device;
    private Button btn_load_patch_code;
    private Button btn_load_config_file;
    private Button btn_start_download_patch;
    private Button btn_read_local_version;

    private ImageButton ib_save_running_log;

    private RecyclerView rv_msg_list;

    private UsbMsgListAdapter mUsbMsgListAdapter;

    private static final int REQUEST_CODE_CHOOSE_PATCH_CODE_FILE = 10001;
    private static final int REQUEST_CODE_CHOOSE_CONFIG_FILE     = 10002;

    private static final int MSG_WHAT_UPDATE_LOG_TEXT            = 0;
    private static final int MSG_WHAT_READ_BINARY_FILE_COMPLETED = 1;
    private static final int MSG_WHAT_ENABLE_SAVE_LOG_BTN        = 2;

    private static final byte[] DEFINE_PATCH_CODE_HEADER = {'R', 'e', 'a', 'l', 't', 'e', 'c', 'h'};

    private static final String TAG = "xp.chen";

    private Uri mSelectedPathCodeUri;
    private Uri mSelectedConfigFileUri;

    private int mMsgCount;

    @Override
    protected void setContainer() {
        setContentView(R.layout.fragment_usb_download_patch);
    }

    @Override
    protected void init() {
        btn_connect_usb_device = findViewById(R.id.btn_connect_usb_device);
        btn_load_patch_code = findViewById(R.id.btn_load_patch_code);
        btn_load_config_file = findViewById(R.id.btn_load_config_file);
        btn_start_download_patch = findViewById(R.id.btn_start_download_patch);
        btn_read_local_version = findViewById(R.id.btn_read_local_version);

        ib_save_running_log = findViewById(R.id.ib_save_running_log);

        rv_msg_list = findViewById(R.id.rv_msg_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_msg_list.setLayoutManager(layoutManager);
        rv_msg_list.setHasFixedSize(true);
    }

    @Override
    protected void setListener() {
        btn_connect_usb_device.setOnClickListener(this);
        btn_load_patch_code.setOnClickListener(this);
        btn_load_config_file.setOnClickListener(this);
        btn_start_download_patch.setOnClickListener(this);
        ib_save_running_log.setOnClickListener(this);
        btn_read_local_version.setOnClickListener(this);
    }


    public static UsbDownloadPatchFragment newInstance() {
        UsbDownloadPatchFragment mFragment = new UsbDownloadPatchFragment();
        Bundle args = new Bundle();
        mFragment.setArguments(args);
        return mFragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mUsbMsgListAdapter == null) {
            mUsbMsgListAdapter = new UsbMsgListAdapter(mContext, new ArrayList<UsbMsg>());
        }
        rv_msg_list.setAdapter(mUsbMsgListAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect_usb_device:
                connectUsbDevice();
                break;
            case R.id.btn_load_patch_code:
                loadPatchCode();
                break;
            case R.id.btn_load_config_file:
                loadConfigFile();
                break;
            case R.id.btn_start_download_patch:
                startDownloadPatch();
                break;
            case R.id.ib_save_running_log:
                saveRunningLog();
                break;
            case R.id.btn_read_local_version:
                readLocalVersionInformationRequest(false);
                break;
            default:
                break;
        }
    }

    private void connectUsbDevice() {
        int initRet = LocalUsbConnector.getInstance().initConnector(mContext);
        if (initRet != UsbError.CODE_NO_ERROR) {
            sendUsbMessage("init usb device failed", initRet);
            return;
        }

        LocalUsbConnector.getInstance().addOnUsbDeviceStatusChangeCallback(mOnUsbDeviceStatusChangeCallback);

        int searchRet = LocalUsbConnector.getInstance().searchUsbDevice();
        if (searchRet != UsbError.CODE_NO_ERROR) {
            sendUsbMessage("can not found usb device", searchRet);
            return;
        }

        int authorizeRet = LocalUsbConnector.getInstance().authorizeDevice();
        if (authorizeRet != UsbError.CODE_NO_ERROR) {
            sendUsbMessage("device is not authorized", authorizeRet);
        }
    }

    private OnUsbDeviceStatusChangeCallback mOnUsbDeviceStatusChangeCallback = new OnUsbDeviceStatusChangeCallback() {
        @Override
        public void authorizeCurrentDevice(UsbDevice usbDevice, boolean authorizeResult) {
            super.authorizeCurrentDevice(usbDevice, authorizeResult);
            if (authorizeResult) {
                int setupRet = LocalUsbConnector.getInstance().setupDevice();
                if (setupRet != UsbError.CODE_NO_ERROR) {
                    sendUsbMessage("usb device setup failed", setupRet);
                    return;
                }

                int connectRet = LocalUsbConnector.getInstance().connect();
                if (connectRet != UsbError.CODE_NO_ERROR) {
                    sendUsbMessage("failed to connect usb device", connectRet);
                    return;
                }
                sendUsbMessage("connected usb device successfully");
            } else {
                sendUsbMessage("usb device authorization failed", UsbMsg.MSG_TYPE_ERROR);
            }
        }

        @Override
        public void onDeviceAttachStatusHasChanged(boolean attachStatus) {
            super.onDeviceAttachStatusHasChanged(attachStatus);
            if (attachStatus) {
                sendUsbMessage("usb device has attached");
            } else {
                sendUsbMessage("usb device has detached", UsbMsg.MSG_TYPE_ERROR);
            }
        }

        @Override
        public void onDeviceConnectionStatusHasChanged(boolean connectionStatus) {
            super.onDeviceConnectionStatusHasChanged(connectionStatus);
        }
    };


    private void loadPatchCode() {
        chooseFile(REQUEST_CODE_CHOOSE_PATCH_CODE_FILE);
    }

    private void loadConfigFile() {
        chooseFile(REQUEST_CODE_CHOOSE_CONFIG_FILE);
    }

    private void startDownloadPatch() {
        // Check local usb device has connected.
        if (LocalUsbConnector.getInstance().getUsbConnectState() != LocalUsbConnector.STATE_USB_CONNECTED) {
            sendUsbMessage(getLocalString(R.string.usb_error_connect_usb_device), UsbMsg.MSG_TYPE_ERROR);
            return;
        }

        // Check whether the patch code file and config file exist
        if (mSelectedPathCodeUri == null) {
            sendUsbMessage(getLocalString(R.string.usb_error_no_patch_code_file), UsbMsg.MSG_TYPE_ERROR);
            return;
        }

        if (mSelectedConfigFileUri == null) {
            sendUsbMessage(getLocalString(R.string.usb_error_no_config_file), UsbMsg.MSG_TYPE_ERROR);
            return;
        }

        sendUsbMessage("Found the patch code file and config file");

        // Check bluetooth gatt connection state (no need when do download patch action)
        // queryBTConnectStateRequest();

        readLocalVersionInformationRequest(true);
    }

    private void queryBTConnectStateRequest() {
        QueryBTConnectStateRequest queryBTConnectStateRequest = new QueryBTConnectStateRequest();
        queryBTConnectStateRequest.addQueryBTConnectStateRequestCallback(new QueryBTConnectStateRequestCallback() {
            @Override
            public void onReceiveConnectState(int statusCode, int connectState) {
                super.onReceiveConnectState(statusCode, connectState);
                if (connectState == UsbGatt.STATE_CONNECTED) {
                    sendUsbMessage("Connect to bluetooth gatt successfully");
                    // readRomVersionCommand(); no need
                    readLocalVersionInformationRequest(true);
                } else {
                    sendUsbMessage("Connect to bluetooth gatt failed, connectState: " + connectState, UsbMsg.MSG_TYPE_ERROR);
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                sendUsbMessage("Connect to bluetooth gatt failed, connection timeout", UsbMsg.MSG_TYPE_ERROR);
            }
        });
        LocalUsbConnector.getInstance().sendRequest(queryBTConnectStateRequest);
    }

    /**
     * Call this method to get the chip id for the current rom.
     */
    private void readRomVersionCommand() {
        ReadRomVersionCommand readRomVersionCommand = new ReadRomVersionCommand();
        readRomVersionCommand.addReadRomVersionCommandCallback(new ReadRomVersionCommandCallback() {
            @Override
            public void onReadRomVersionSuccess(int romVersion) {
                super.onReadRomVersionSuccess(romVersion);
                sendUsbMessage("current rom version is: " + romVersion + " + 1");
                new ReadBinaryFileContentThread(romVersion).start();
                // Compare Bluetooth chip versions
                // readLocalVersionInformationRequest();
                // Start load patch code file and config file
            }

            @Override
            public void onReadRomVersionFail() {
                super.onReadRomVersionFail();
                sendUsbMessage("read rom version failed");
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                sendUsbMessage("read rom version timeout");
            }
        });
        LocalUsbConnector.getInstance().sendRequest(readRomVersionCommand);
    }


    private void readLocalVersionInformationRequest(boolean isNeedDownload) {
        ReadLocalChipVersionInfoRequest readLocalChipVersionInfoRequest = new ReadLocalChipVersionInfoRequest();
        readLocalChipVersionInfoRequest.addReadLocalChipVersionInfoRequestCallback(new ReadLocalChipVersionInfoRequestCallback() {
            @Override
            public void onReceivedVersionInformation(int hciVersion, int hciRevision, int lmpVersion, int lmpSubVersion, int manufacturerName) {
                super.onReceivedVersionInformation(hciVersion, hciRevision, lmpVersion, lmpSubVersion, manufacturerName);
                String versionInfo = String.format(Locale.getDefault(), "hciVersion: %#x, hciRevision: %#x, lmpVersion: %#x, lmpSubVersion: %#x, manufacturerName: %#x",
                        hciVersion, hciRevision, lmpVersion, lmpSubVersion, manufacturerName);
                sendUsbMessage("hci read local version info: " + versionInfo);

                boolean needDownload = checkIsNeedDownloadPathByCompareVersion(lmpSubVersion, hciRevision);
                if (needDownload) {
                    sendUsbMessage("lmpSubversion match, able to download patch");
                    if (isNeedDownload) {
                        new ReadBinaryFileContentThread(0).start();
                    }
                } else {
                    sendUsbMessage("lmpSubversion not match, No download required");
                }
                // TODO: 2020/1/13 Start compare bluetooth chip version,
                // new ReadBinaryFileContentThread().start();
            }

            @Override
            public void onReceiveFailed() {
                super.onReceiveFailed();
                sendUsbMessage("hci read local version failed", UsbMsg.MSG_TYPE_ERROR);
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                sendUsbMessage("hci read local version timeout", UsbMsg.MSG_TYPE_ERROR);
            }

        });
        LocalUsbConnector.getInstance().sendRequest(readLocalChipVersionInfoRequest);
    }


    private boolean checkIsNeedDownloadPathByCompareVersion(int lmpSubVersion, int hciRevision) {
        int versionInfo[][] = RtkBTChipVersionInfo.CHIP_VERSION_INFO_TABLE;
        for (int[] chipVersion : versionInfo) {
            /*if (lmpSubVersion == chipVersion[0] && hciRevision == chipVersion[1]) {
                return true;
            }*/
            // allow download Only lmpSubVersion is equals.
            if (lmpSubVersion == chipVersion[0]) {
                return true;
            }
        }
        return false;
    }

    private class ReadBinaryFileContentThread extends Thread {

        /**
         * chip id of the current rom
         */
        private int mChipID;

        ReadBinaryFileContentThread(int chipID) {
            // hci_rtk_find_patch(rom_version + 1, path_info, hci_process_info);
            // find patch by this chip id
            // this.mChipID = chipID + 1;
            // TODO: 2020/1/15 There is no release version of patch firmware now,
            //  so I set the chip id value to 2 for debug.
            this.mChipID = 2;
        }

        @Override
        public void run() {
            super.run();
            // Read binary file content
            byte[] patchCodeByteArr = FileUtil.readBinaryFileContent(mContext, mSelectedPathCodeUri);
            if (patchCodeByteArr == null) {
                sendUsbMessage("can not read patch code file", UsbMsg.MSG_TYPE_ERROR);
                return;
            }

            byte[] configFileByteArr = FileUtil.readBinaryFileContent(mContext, mSelectedConfigFileUri);
            if (configFileByteArr == null) {
                sendUsbMessage("can not read config file", UsbMsg.MSG_TYPE_ERROR);
                return;
            }
            // Merge Patch code byte array and config file byte array to a new array.
            byte[] mergedPatchCode = new byte[patchCodeByteArr.length + configFileByteArr.length];
            System.arraycopy(patchCodeByteArr, 0, mergedPatchCode, 0, patchCodeByteArr.length);
            System.arraycopy(configFileByteArr, 0, mergedPatchCode, patchCodeByteArr.length, configFileByteArr.length);

            // Check whether the patch header is correct
            // Note: Download Patch Code Header must be "Realtech"

            /*byte[] patchCodeHeader = Arrays.copyOfRange(mergedPatchCode, 0, DEFINE_PATCH_CODE_HEADER.length);
            if (!Arrays.equals(patchCodeHeader, DEFINE_PATCH_CODE_HEADER)) {
                sendUsbMessage("download failed, invalid patch code file", UsbMsg.MSG_TYPE_ERROR);
                return;
            }*/

            // Read version info(LmpSubversion)
            /*ByteBuffer buffer = ByteBuffer.wrap(mergedPatchCode);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int patch_lmp_subversion = buffer.getInt(8);
            int patch_len = 0, patch_offset = 0;

            int num_of_patch = (mergedPatchCode[0x0C] | (mergedPatchCode[0x0D] << 8)) & 0x0FF;
            sendUsbMessage("patch num in file is: " + num_of_patch);

            if (num_of_patch == 1) {
                // If there is only one patch
                int firmware_chip_id = (mergedPatchCode[0x0E] | (mergedPatchCode[0x0F] << 8)) & 0x0FF;
                // Note: This tool is only for 8761Bru Dongle, No need to call the {@link ReadRomVersionCommand} interface,
                // no chip_id exists.
                // sendUsbMessage("firmware chip id: " + firmware_chip_id + ", read rom chip id: " + mChipID);
                sendUsbMessage("firmware chip id: " + firmware_chip_id);

                patch_len = (mergedPatchCode[0x0E + 2] | (mergedPatchCode[0x0F + 2] << 8)) & 0x0FF;
                patch_offset = (mergedPatchCode[0x0E + 4] | (mergedPatchCode[0x0F + 4] << 8)) & 0x0FF;
            } else {
                // If there are multiple patches
                int i = 0;
                for (i = 0; i < num_of_patch; i++) {
                    int firmware_chip_id = (mergedPatchCode[0x0E + 2 * i] | (mergedPatchCode[0x0F + 2 * i] << 8)) & 0x0FF;
                    sendUsbMessage("find firmware chip id " + firmware_chip_id + "...");
                    if (firmware_chip_id == mChipID) {
                        patch_len = (mergedPatchCode[0x0e + 2 * num_of_patch + 2 * i] | (mergedPatchCode[0x0f + 2 * num_of_patch + 2 * i] << 8)) & 0x0FF;
                        patch_offset = (mergedPatchCode[0x0e + 4 * num_of_patch + 4 * i] | (mergedPatchCode[0x0f + 4 * num_of_patch + 4 * i] << 8)) & 0X0FF;
                        break;
                    }
                }
                // If can not found the matching patch id, then tell the user to end
                if (i >= num_of_patch) {
                    sendUsbMessage("matching chip id could not be found", UsbMsg.MSG_TYPE_ERROR);
                    return;
                }
            }

            // Create a new byte array to hold the patch info to be sent
            byte[] patch_info = new byte[patch_len];
            System.arraycopy(mergedPatchCode, patch_offset, patch_info, 0, patch_info.length);
            ByteBuffer patch_info_buffer = ByteBuffer.wrap(patch_info);
            patch_info_buffer.order(ByteOrder.LITTLE_ENDIAN);
            patch_info_buffer.putInt(patch_info.length - 4, patch_lmp_subversion);

            sendUsbMessage("patch file read complete(length = " + patch_len + "), ready to start...");
*/
            // Ready to start send file
            sendPacket2BtController(mergedPatchCode);
        }


        private void sendPacket2BtController(byte[] patchArray) {
            DownloadPatchHelper downloadPatchHelper = new DownloadPatchHelper(patchArray);
            downloadPatchHelper.addOnDownloadStatusChangeListener(new DownloadPatchHelper.OnDownloadStatusChangeListener() {
                @Override
                public void onDownloadStarted() {
                    sendUsbMessage("start the download patch operation...");
                }

                @Override
                public void onDownloadCanceled() {
                    sendUsbMessage("download patch has been canceled", UsbMsg.MSG_TYPE_ERROR);
                }

                @Override
                public void onDownloadFailed(int errorCode) {
                    sendUsbMessage("download patch failed", errorCode);
                }

                @Override
                public void onDownloadCompleted() {
                    sendUsbMessage("download patch has completed");
                }

                @Override
                public void onDownloadProgressChanged(int progress) {
                    sendUsbMessage("download patch... " + progress + "%");
                }
            });
            downloadPatchHelper.startDownloadPatch();
        }
    }

    private void chooseFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    private void sendUsbMessage(String message) {
        sendUsbMessage(message, UsbMsg.MSG_TYPE_NORMAL);
    }

    private void sendUsbMessage(String message, int errorCode) {
        UsbMsg usbMsg = new UsbMsg(message, errorCode);
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_WHAT_UPDATE_LOG_TEXT;
        msg.obj = usbMsg;
        msg.sendToTarget();
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_WHAT_UPDATE_LOG_TEXT:
                    UsbMsg usbMsg = (UsbMsg) msg.obj;
                    mUsbMsgListAdapter.addMsgItem(usbMsg);
                    mUsbMsgListAdapter.notifyItemInserted(0);
                    rv_msg_list.scrollToPosition(0);
                    break;
                case MSG_WHAT_READ_BINARY_FILE_COMPLETED:

                    break;
                case MSG_WHAT_ENABLE_SAVE_LOG_BTN:
                    ib_save_running_log.setEnabled(true);
                    if (msg.arg1 < 0) {
                        showToast("Save log failed");
                    } else {
                        showToast("Save log success");
                    }
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_CODE_CHOOSE_PATCH_CODE_FILE && resultCode == Activity.RESULT_OK) {
            if (resultData == null) return;
            Uri uri = resultData.getData();

            if (uri == null) return;
            Log.i(TAG, "Select Patch Code Uri: " + uri.toString());

            mSelectedPathCodeUri = uri;
            sendUsbMessage("Select Patch Code Uri: " + uri.toString());
        }

        if (requestCode == REQUEST_CODE_CHOOSE_CONFIG_FILE && resultCode == Activity.RESULT_OK) {
            if (resultData == null) return;
            Uri uri = resultData.getData();

            if (uri == null) return;
            Log.i(TAG, "Select Config File uri: " + uri.toString());

            mSelectedConfigFileUri = uri;
            sendUsbMessage("Select Config File uri: " + uri.toString());
        }
    }


    private class SaveRunningLogWorker extends Thread {

        private String       mSavePath;
        private List<UsbMsg> mUsbMsgList;

        SaveRunningLogWorker(String savePath, List<UsbMsg> usbMsgList) {
            this.mSavePath = savePath;
            this.mUsbMsgList = usbMsgList;
        }

        @Override
        public void run() {
            super.run();
            Message msg = mHandler.obtainMessage(MSG_WHAT_ENABLE_SAVE_LOG_BTN);

            if (mUsbMsgList == null || mUsbMsgList.size() == 0) {
                msg.arg1 = -1;
                msg.sendToTarget();
                return;
            }
            StringBuilder builder = new StringBuilder();
            String fileHeaderStr = "[" + TimeUtil.getFullTimeStr() + "]";
            String newLineSymbol = "\r\n";
            builder.append(fileHeaderStr).append(newLineSymbol);
            // Record detail content
            for (UsbMsg usbMsg : mUsbMsgList) {
                builder.append(usbMsg.getMsgString()).append(newLineSymbol);
            }
            boolean saveResult = FileUtil.saveContent2File(mSavePath, builder.toString());
            if (saveResult) {
                msg.arg1 = 0;
                mMsgCount = mUsbMsgList.size();
            } else {
                msg.arg1 = -1;
            }
            msg.sendToTarget();
        }
    }

    private void saveRunningLog() {
        ib_save_running_log.setEnabled(false);
        // Create a new thread to perform the save operation.
        String filePrefix = "Download_Patch_";
        String fileSuffix = ".txt";
        String fileName = filePrefix + TimeUtil.getSimpleDateStr() + fileSuffix;

        String saveLogDirPath = PathDefine.ROOT_PATH + File.separator +
                PathDefine.DONGLE_APP_PATH + File.separator +
                PathDefine.DOWNLOAD_PATCH_LOG_PATH;
        File saveLogDir = new File(saveLogDirPath);
        if (!saveLogDir.exists()) saveLogDir.mkdirs();

        String saveLogName = saveLogDirPath + File.separator + fileName;
        int realMsgCount = mUsbMsgListAdapter.getItemCount();
        if (realMsgCount == 0) {
            ib_save_running_log.setEnabled(true);
            return;
        }

        // Save only if the log contents are different.
        if (mMsgCount == realMsgCount) {
            Message msg = mHandler.obtainMessage(MSG_WHAT_ENABLE_SAVE_LOG_BTN);
            msg.arg1 = 0;
            msg.sendToTarget();
            return;
        }
        new SaveRunningLogWorker(saveLogName, mUsbMsgListAdapter.getMsgList()).start();
    }

}
