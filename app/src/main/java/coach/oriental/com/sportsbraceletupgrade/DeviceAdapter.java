package coach.oriental.com.sportsbraceletupgrade;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceAdapter extends BaseAdapter {
    private ArrayList<Device> devices;
    private Context mContext;
    private LayoutInflater inflater;

    public DeviceAdapter(Context context, ArrayList<Device> devices) {
        this.devices = devices;
        mContext = context;
        inflater = LayoutInflater.from(mContext);
    }

    public void setDevices(ArrayList<Device> devices) {
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Device device = devices.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.device_item, parent, false);
            holder = new ViewHolder();
            holder.tv_device_name = (TextView) convertView
                    .findViewById(R.id.tv_device_name);
            holder.tv_device_status = (TextView) convertView
                    .findViewById(R.id.tv_device_status);
            holder.tv_device_rssi = (TextView) convertView
                    .findViewById(R.id.tv_device_rssi);
            holder.tv_device_version = (TextView) convertView
                    .findViewById(R.id.tv_device_version);
            holder.tv_device_address = (TextView) convertView
                    .findViewById(R.id.tv_device_address);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tv_device_name.setText(device.name);
        if (device.status == Device.STATUS_CONN_FALSE) {
            holder.tv_device_status.setText("未连接");
        } else if (device.status == Device.STATUS_UPGRADE_ING) {
            holder.tv_device_status.setText("正在升级");
        } else if (device.status == Device.STATUS_CONN_ING) {
            holder.tv_device_status.setText("正在连接");
        } else if (device.status == Device.STATUS_GET_VERSION) {
            holder.tv_device_status.setText("正在获取版本号");
        } else if (device.status == Device.STATUS_UPGRADE_SUCCESS) {
            holder.tv_device_status.setText("升级成功");
        }
        holder.tv_device_rssi.setText(device.rssi + "");
        holder.tv_device_version.setText(device.version);
        holder.tv_device_address.setText(!TextUtils.isEmpty(device.address) ? device.address : "");
        return convertView;
    }

    class ViewHolder {
        TextView tv_device_name;
        TextView tv_device_status;
        TextView tv_device_version;
        TextView tv_device_rssi;
        TextView tv_device_address;
    }
}
