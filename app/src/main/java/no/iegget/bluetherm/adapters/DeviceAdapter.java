package no.iegget.bluetherm.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import no.iegget.bluetherm.R;
import no.iegget.bluetherm.models.BluetoothDevice;

/**
 * Created by iver on 22/04/16.
 */
public class DeviceAdapter<S> extends ArrayAdapter<BluetoothDevice> {

    private ViewHolder viewHolder;

    private static class ViewHolder {
        private TextView itemView;
    }

    public DeviceAdapter(Context context, int resource, int textViewResourceId, List<BluetoothDevice> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.device_row_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.itemView = (TextView) convertView.findViewById(R.id.deviceListText);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice item = getItem(position);
        if (item!= null) {
            // My layout has only one TextView
            // do whatever you want with your string and long
            viewHolder.itemView.setText(item.getName() + " - " + item.getAddress());
        }

        return convertView;
    }
}
