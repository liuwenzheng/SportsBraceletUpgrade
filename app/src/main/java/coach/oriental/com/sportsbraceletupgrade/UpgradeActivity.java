package coach.oriental.com.sportsbraceletupgrade;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class UpgradeActivity extends Activity implements OnClickListener, AdapterView.OnItemClickListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_FILE = 2;

    @InjectView(R.id.btn_upgrade)
    Button btn_upgrade;
    @InjectView(R.id.btn_file)
    Button btn_file;
    @InjectView(R.id.btn_refresh)
    Button btnRefresh;
    @InjectView(R.id.et_scan_period)
    EditText etScanPeriod;
    @InjectView(R.id.lv_devices)
    ListView lvDevices;
    @InjectView(R.id.et_filter_name)
    EditText etFilterName;
    @InjectView(R.id.et_over_time)
    EditText etOverTime;
    @InjectView(R.id.et_filter_rssi)
    EditText etFilterRssi;
    //    @InjectView(R.id.et_filter_version)
//    EditText et_filter_version;
    @InjectView(R.id.tv_file_name)
    TextView tv_file_name;
//    @InjectView(R.id.tv_version)
//    TextView tv_version;
    private DeviceAdapter mAdapter;
    private ArrayList<Device> devices;
    private BTService mBtService;
    private ProgressDialog mDialog;
    private HashMap<String, Device> devicesMaps;
    private long mOverTime;
    private int mFilterRssi;
    private Device mDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , PERMISSION_REQUEST_CODE);
                return;
            }
        }
        initContentView();
    }

    private void initContentView() {
        setContentView(R.layout.upgrade);
        ButterKnife.inject(this);
        SPUtiles.getInstance(this);
        // 启动蓝牙服务
        // startService(new Intent(this, BTService.class));
        // 初始化蓝牙适配器
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BTModule.mBluetoothAdapter = bluetoothManager.getAdapter();
        devices = new ArrayList<>();
        devicesMaps = new HashMap<>();
        mAdapter = new DeviceAdapter(this, devices);
        lvDevices.setAdapter(mAdapter);
        lvDevices.setOnItemClickListener(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA);
        filter.addAction(BTConstants.ACTION_BLE_DEVICES_DATA_END);
        filter.addAction(BTConstants.ACTION_CONN_STATUS_TIMEOUT);
        filter.addAction(BTConstants.ACTION_CONN_STATUS_DISCONNECTED);
        filter.addAction(BTConstants.ACTION_DISCOVER_SUCCESS);
        filter.addAction(BTConstants.ACTION_DISCOVER_FAILURE);
        filter.addAction(BTConstants.ACTION_REFRESH_DATA);
        filter.addAction(BTConstants.ACTION_ACK);
        registerReceiver(mReceiver, filter);
        bindService(new Intent(this, BTService.class), mServiceConnection,
                BIND_AUTO_CREATE);
        btn_upgrade.setEnabled(false);
        etOverTime.setText(SPUtiles.getStringValue("overTime", "10"));
        etFilterName.setText(SPUtiles.getStringValue("filterName", ""));
        etScanPeriod.setText(SPUtiles.getStringValue("scanPeriod", "5"));
        etFilterRssi.setText(SPUtiles.getStringValue("filterRssi", "-96"));
//        et_filter_version.setText(SPUtiles.getStringValue("filterVersion", ""));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        ToastUtils.showToast(UpgradeActivity.this, "This app needs these permissions!");
                        UpgradeActivity.this.finish();
                        return;
                    }
                }
                initContentView();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // 注销广播接收器
        unregisterReceiver(mReceiver);
        mBtService.disConnectBle();
        unbindService(mServiceConnection);
        mBtService = null;
        if (in != null) {
            try {
                in.close();
                in = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // stopService(new Intent(this, BTService.class));
        super.onDestroy();
    }

    /**
     * 同步数据
     */
//    private void synData() {
//        // 5.0偶尔会出现获取不到数据的情况，这时候延迟发送命令，解决问题
//        BTService.mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mBtService.synSleep();
//            }
//        }, 200);
//    }

    private InputStream in;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (BTConstants.ACTION_BLE_DEVICES_DATA.equals(intent
                        .getAction())) {
                    Device bleDevice = intent.getParcelableExtra("device");
                    String filterName = etFilterName.getText().toString();
                    if (!TextUtils.isEmpty(filterName)
                            && !bleDevice.name.equals(filterName)) {
                        return;
                    }
                    int rssi = Integer.valueOf(bleDevice.rssi);
                    if (rssi <= mFilterRssi || rssi > 4) {
                        return;
                    }
//                    if (!TextUtils.isEmpty(et_filter_version.getText().toString())) {
//                        byte[] scanRecord = bleDevice.scanRecord;
//                        StringBuilder sb = new StringBuilder();
//                        for (int i = 0; i < scanRecord.length; i++) {
//                            sb.append(Utils.byte2HexString(scanRecord[i]));
//                            if (i < scanRecord.length - 1) {
//                                sb.append(" ");
//                            }
//                            if (i % 15 == 1)
//                                sb.append("\n");
//                        }
//                        LogModule.i(sb.toString());
//                        int index = 0;
//                        for (int i = 0; i < scanRecord.length; i++) {
//                            if ("0a".equals(Utils.byte2HexString(scanRecord[i]))
//                                    && "ff".equals(Utils.byte2HexString(scanRecord[i + 1]))) {
//                                index = i + 8;
//                                break;
//                            }
//                        }
//                        if (index == 0) {
//                            return;
//                        }
//                        LogModule.i(index + "");
//                        LogModule.i("手环固件版本号：" + Utils.byte2HexString(scanRecord[index])
//                                + Utils.byte2HexString(scanRecord[index + 1])
//                                + Utils.byte2HexString(scanRecord[index + 2]));
//                        LogModule.i(et_filter_version.getText().toString() + "");
//                        String[] s = et_filter_version.getText().toString().split("\\.");
//                        if (s.length != 3) {
//                            return;
//                        }
//                        String s1 = Utils.decodeToHex(s[0]);
//                        String s2 = Utils.decodeToHex(s[1]);
//                        String s3 = Utils.decodeToHex(s[2]);
//                        if (s1.length() == 1) {
//                            s1 = "0" + s1;
//                        }
//                        if (s2.length() == 1) {
//                            s2 = "0" + s2;
//                        }
//                        if (s3.length() == 1) {
//                            s3 = "0" + s3;
//                        }
//                        LogModule.i("filter version : " + s1 + "." + s2 + "." + s3);
//                        if (s1.equals(Utils.byte2HexString(scanRecord[index]))
//                                && s2.equals(Utils.byte2HexString(scanRecord[index + 1]))
//                                && s3.equals(Utils.byte2HexString(scanRecord[index + 2]))) {
//                            if (!devicesMaps.containsKey(bleDevice.address)) {
//                                devicesMaps.put(bleDevice.address, bleDevice);
//                                devices.add(bleDevice);
//                            } else {
//                                return;
//                            }
//                            Collections.sort(devices);
//                            mAdapter.setDevices(devices);
//                            mAdapter.notifyDataSetChanged();
//                            if (devices.size() >= 30) {
//                                mBtService.stopLeScan();
//                            }
//                        }
//                    } else {
                    if (!devicesMaps.containsKey(bleDevice.address)) {
                        devicesMaps.put(bleDevice.address, bleDevice);
                        devices.add(bleDevice);
                    } else {
                        return;
                    }
                    Collections.sort(devices);
                    mAdapter.setDevices(devices);
                    mAdapter.notifyDataSetChanged();
                    if (devices.size() >= 30) {
                        mBtService.stopLeScan();
                    }
//                    }
                }
                if (BTConstants.ACTION_BLE_DEVICES_DATA_END.equals(intent
                        .getAction())) {
                    mAdapter.notifyDataSetChanged();
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                    if (devices.isEmpty()) {
                        btnRefresh.setEnabled(true);
                        btn_upgrade.setEnabled(false);
                        return;
                    }
                    btnRefresh.setEnabled(true);
                    btn_upgrade.setEnabled(true);
                }
                if (BTConstants.ACTION_CONN_STATUS_TIMEOUT.equals(intent
                        .getAction())
                        || BTConstants.ACTION_CONN_STATUS_DISCONNECTED
                        .equals(intent.getAction())
                        || BTConstants.ACTION_DISCOVER_FAILURE.equals(intent
                        .getAction())) {
                    if (in != null) {
                        try {
                            in.close();
                            in = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mDialog != null)
                        mDialog.dismiss();
//                    if (devices.isEmpty())
//                        return;
//                    isError();
                    mDevice.status = Device.STATUS_CONN_FALSE;
                    mAdapter.notifyDataSetChanged();
                    ToastUtils.showToast(UpgradeActivity.this, "配对失败");
                }
                if (BTConstants.ACTION_DISCOVER_SUCCESS.equals(intent
                        .getAction())) {
//                    if (devices.isEmpty())
//                        return;
                    if (devicesMaps.containsKey(mDevice.address)) {
                        mAdapter.notifyDataSetChanged();
                        // 连接超时
                        LogModule.i("配对成功...");
                        ToastUtils.showToast(UpgradeActivity.this, "配对成功");
                        if (mDialog != null)
                            mDialog.dismiss();
//                        mDialog = ProgressDialog.show(UpgradeActivity.this, null, "获取版本号", false, false);
//                        mDevice.status = Device.STATUS_CRC;
//                        mAdapter.notifyDataSetChanged();
//                        mBtService.getVersion();
                        // 升级固件
                        // upgradeDevice(device);
                        // device.status = Device.STATUS_UPGRADE_ING;
                        // synData();
                        mDevice.status = Device.STATUS_CRC;
                        mAdapter.notifyDataSetChanged();
                        try {
                            mDialog = ProgressDialog.show(UpgradeActivity.this, null, "CRC校验", false, false);
                            mBtService.getCRCResult(tv_file_name.getText().toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mDevice.status = Device.STATUS_CONN_FALSE;
                            mAdapter.notifyDataSetChanged();
                            ToastUtils.showToast(UpgradeActivity.this, "CRC校验异常");
                        }
                    }
                }
                if (BTConstants.ACTION_ACK.equals(intent.getAction())) {
                    int ack = intent.getIntExtra(
                            BTConstants.EXTRA_KEY_ACK_VALUE, 0);
                    if (ack == 0) {
                        return;
                    }
                    if (ack == BTConstants.HEADER_BACK_PACKAGE) {
                        final byte[] index = intent.getByteArrayExtra(BTConstants.EXTRA_KEY_PACKAGE_INDEX);
                        if (Utils.toInt(index) == 0) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    int i = 0;
                                    try {
                                        File file = new File(tv_file_name.getText().toString());
                                        if (in == null) {
                                            in = new FileInputStream(file);
                                        }
                                        while (in.available() > 0) {
                                            byte[] indexByte = Utils.toByteArray(i, 2);
                                            byte b[] = new byte[17];
                                            in.read(b);
                                            mBtService.sendPackage(indexByte, b);
                                            i++;
                                            Thread.sleep(20);
                                        }
                                        in.close();
                                        in = null;
                                    } catch (Exception e) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mDevice.status = Device.STATUS_CONN_FALSE;
                                                mAdapter.notifyDataSetChanged();
                                                if (mDialog != null)
                                                    mDialog.dismiss();
                                                ToastUtils.showToast(UpgradeActivity.this, "发送包异常");
//                                                if (devices.isEmpty())
//                                                    return;
//                                                isError();
                                            }
                                        });
                                    }
                                }
                            }).start();
//                                ToastUtils.showToast(UpgradeActivity.this, "接收到的序号：" + Utils.toInt(index));
                            mDevice.status = Device.STATUS_UPGRADE_ING;
                            mAdapter.notifyDataSetChanged();
                            ToastUtils.showToast(UpgradeActivity.this, "开始发送数据包");
                        }
                        return;
                    }
                    if (ack == BTConstants.HEADER_BACK_PACKAGE_RESULT) {
                        if (mDialog != null)
                            mDialog.dismiss();
                        int result = intent.getIntExtra(BTConstants.EXTRA_KEY_PACKAGE_RESULT, -1);
                        switch (result) {
                            case 0:
                                ToastUtils.showToast(UpgradeActivity.this, "升级成功");
                                break;
                            case 1:
                                ToastUtils.showToast(UpgradeActivity.this, "升级超时");
                                break;
                            case 2:
                                ToastUtils.showToast(UpgradeActivity.this, "升级校验码错误");
                                break;
                            case 3:
                                ToastUtils.showToast(UpgradeActivity.this, "升级文件错误");
                                break;
                            default:
                                ToastUtils.showToast(UpgradeActivity.this, "未知错误");
                                break;
                        }
                        if (result != 0) {
                            mDevice.status = Device.STATUS_CONN_FALSE;
                            mAdapter.notifyDataSetChanged();
                        } else {
                            mDevice.status = Device.STATUS_UPGRADE_SUCCESS;
                            mAdapter.notifyDataSetChanged();
                        }
//                        if (devices.isEmpty())
//                            return;
//                        isError();
                        return;
                    }
                    if (ack == BTConstants.HEADER_CRC) {
                        if (mDialog != null)
                            mDialog.dismiss();
                        mDialog = ProgressDialog.show(UpgradeActivity.this, null, "正在升级...", false, false);
                        ToastUtils.showToast(UpgradeActivity.this, "CRC校验正常，开始升级");
                        return;
                    }
//                    if (ack == BTConstants.HEADER_BACK_ACK) {
//                        if (mDialog != null)
//                            mDialog.dismiss();
//                        String version = SPUtiles.getStringValue(BTConstants.SP_KEY_DEVICE_VERSION, "");
//                        if (!TextUtils.isEmpty(version)) {
//                            mDevice.version = version;
//                            mDevice.status = Device.STATUS_UPGRADE_ING;
//                            mAdapter.notifyDataSetChanged();
//                            try {
//                                boolean canUpgrade = canUpgrade(version, tv_version.getText().toString());
//                                if (canUpgrade) {
                    // ToastUtils.showToast(UpgradeActivity.this, "可以升级");

//                                } else {
//                                    if (devices.isEmpty())
//                                        return;
//                                    isError();
//                                    ToastUtils.showToast(UpgradeActivity.this, "不允许升级");
//                                }
//                            } catch (Exception e) {
//                                if (devices.isEmpty())
//                                    return;
//                                isError();
//                                ToastUtils.showToast(UpgradeActivity.this, "比较版本失败");
//                            }
//                        } else {
//                            if (devices.isEmpty())
//                                return;
//                            isError();
//                            ToastUtils.showToast(UpgradeActivity.this, "获取手环固件版本号失败");
//                        }

//                    }

//                    if (ack == 0x96) {
//                        if (devices.isEmpty())
//                            return;
//                        Device device = devices.get(0);
//                        if (devicesMaps.containsKey(device.address)) {
//                            removeDevice();
//                            mAdapter.notifyDataSetChanged();
//                            LogModule.i("固件升级成功...");
//                            ToastUtils.showToast(UpgradeActivity.this, "固件升级成功");
//                            if (mDialog != null)
//                                mDialog.dismiss();
//                            // 关闭手环并删除
//                            // cnnDevice();
//                        }
//                    }
                }
            }

        }
    };

//    private void isError() {
//        Device device = devices.get(0);
//        if (devicesMaps.containsKey(device.address)) {
//            removeDevice();
//            mAdapter.notifyDataSetChanged();
//            // 关闭手环并删除
//            connDevice(device);
//        }
//    }

//    private boolean canUpgrade(String srcVersion, String targetVersion) throws Exception {
//        try {
//            String[] src = srcVersion.split("\\.");
//            String[] target = targetVersion.split("\\.");
//            if (!src[0].equals(target[0])) {
//                return false;
//            } else {
//                String srcHeight = src[1].substring(0, 1);
//                String srcLow = src[1].substring(1, 2);
//                String tarHeight = target[1].substring(0, 1);
//                String tarLow = target[1].substring(1, 2);
//                if (!srcHeight.equals(tarHeight)) {
//                    return false;
//                } else {
//                    int src2 = Integer.parseInt(srcLow, 16);
//                    int target2 = Integer.parseInt(tarLow, 16);
//                    if (target2 > src2) {
//                        return true;
//                    } else if (target2 < src2) {
//                        return false;
//                    } else {
//                        int src3 = Integer.parseInt(src[2], 16);
//                        int target3 = Integer.parseInt(target[2], 16);
//                        if (target3 > src3) {
//                            return true;
//                        } else {
//                            return false;
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            throw e;
//        }
//    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogModule.d("连接服务onServiceConnected...");
            mBtService = ((BTService.LocalBinder) service).getService();
            if (mBtService.mBluetoothGatt != null) {
                mBtService.disConnectBle();
            }
            // 开启蓝牙
            if (!BTModule.isBluetoothOpen()) {
                BTModule.openBluetooth(UpgradeActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogModule.d("断开服务onServiceDisconnected...");
            mBtService = null;
        }
    };

    @OnClick({R.id.btn_upgrade, R.id.btn_refresh, R.id.btn_file})
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.btn_upgrade:
//                if (TextUtils.isEmpty(tv_file_name.getText().toString())) {
//                    ToastUtils.showToast(this, "请先选择固件文件");
//                    return;
//                }
//                btn_upgrade.setEnabled(false);
//                String overTime = etOverTime.getText().toString();
//                String filterName = etFilterName.getText().toString();
//                SPUtiles.setStringValue("overTime", overTime);
//                SPUtiles.setStringValue("filterName", filterName);
//                mOverTime = TextUtils.isEmpty(overTime) ? 2 * 1000 : Integer.parseInt(overTime) * 1000;
//                connDevice(device);
//                break;
            case R.id.btn_refresh:
                mDialog = ProgressDialog.show(UpgradeActivity.this, null,
                        getString(R.string.scan_device), false, false);
                devicesMaps.clear();
                devices.clear();
                mAdapter.notifyDataSetChanged();
                // 扫描设备s
                mBtService.scanDevice(Integer.parseInt(etScanPeriod.getText().toString()));
                btnRefresh.setEnabled(false);
                SPUtiles.setStringValue("scanPeriod", etScanPeriod.getText().toString());
                String filterRssi = etFilterRssi.getText().toString();
                SPUtiles.setStringValue("filterRssi", filterRssi);
//                String filterVersion = et_filter_version.getText().toString();
//                SPUtiles.setStringValue("filterVersion", filterVersion);
                mFilterRssi = TextUtils.isEmpty(filterRssi) ? -96 : Integer.parseInt(filterRssi);
                break;
            case R.id.btn_file:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "请选择文件!"), REQUEST_CODE_FILE);
                } catch (ActivityNotFoundException ex) {
                    ToastUtils.showToast(this, "请安装文件管理器");
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FILE:
                    //得到uri，后面就是将uri转化成file的过程。
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
//                    try {
//                        String[] version = FileUtils.getVersion(path);
//                        tv_version.setText(String.format("%s.%s.%s", version[0], version[1], version[2]));
                        Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                        tv_file_name.setText(path);
//                    } catch (Exception e) {
//                        Toast.makeText(this, "无法获取待升级固件版本号", Toast.LENGTH_SHORT).show();
//                    }
                    break;
            }
        }
    }

    private void connDevice() {
        mBtService.disConnectBle();
        mDialog = ProgressDialog.show(UpgradeActivity.this, null,
                getString(R.string.match_device), false, false);
        mBtService.connectBle(mDevice.address);
        mDevice.status = Device.STATUS_CONN_ING;
        mAdapter.notifyDataSetChanged();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        if (devices.isEmpty())
//                            return;
                        if (mDevice.status == 1) {
//                            removeDevice();
//                            mAdapter.notifyDataSetChanged();
                            // 连接超时
                            LogModule.i("连接超时...");
                            ToastUtils.showToast(UpgradeActivity.this, "连接超时");
                            if (mDialog != null)
                                mDialog.dismiss();
                            mDevice.status = Device.STATUS_CONN_FALSE;
                            mAdapter.notifyDataSetChanged();
                            // 关闭手环并删除
//                            connDevice();
                        }
                    }
                });
            }
        }, mOverTime);
    }

//    private void upgradeDevice(final Device device) {
//        mDialog = ProgressDialog.show(UpgradeActivity.this, null,
//                getString(R.string.upgrade_device), false, false);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (devices.isEmpty())
//                            return;
//                        if (devicesMaps.containsKey(device.address)) {
//                            removeDevice();
//                            mAdapter.notifyDataSetChanged();
//                            // 连接超时
//                            LogModule.i("关闭超时...");
//                            ToastUtils.showToast(UpgradeActivity.this, "关闭超时");
//                            if (mDialog != null)
//                                mDialog.dismiss();
//                            // 关闭手环并删除
//                            // connDevice();
//                        }
//                    }
//                });
//            }
//        }, mOverTime);
//    }


//    private synchronized void removeDevice() {
//        Device device = devices.get(0);
//        devices.remove(device);
//        devicesMaps.remove(device.address);
//    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (TextUtils.isEmpty(tv_file_name.getText().toString())) {
            ToastUtils.showToast(this, "请先选择固件文件");
            return;
        }
        mDevice = (Device) parent.getItemAtPosition(position);
        String overTime = etOverTime.getText().toString();
        String filterName = etFilterName.getText().toString();
        SPUtiles.setStringValue("overTime", overTime);
        SPUtiles.setStringValue("filterName", filterName);
        mOverTime = TextUtils.isEmpty(overTime) ? 2 * 1000 : Integer.parseInt(overTime) * 1000;
        connDevice();
    }
}
