package br.net.triangulohackerspace.mariakart;


import java.util.Set;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListBluetoothActivity extends ListActivity {
	
	private BluetoothAdapter btAdapter = null;
	
	private static final String TAG = "bluetooth1";
	
	Set<BluetoothDevice> pairedDevices;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_bluetooth,getBluetoothPairedDevices()));

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
			    
				Log.d(TAG, "Selecionado: "+((TextView) view).getText().toString());
				
				Intent resultIntent = new Intent(ListBluetoothActivity.this, MainActivity.class);
				resultIntent.putExtra("BLUETOOTH_MAC", ((TextView) view).getText());
				setResult(Activity.RESULT_OK, resultIntent);
				
				finish();
				
			}
		});
	}

	private String[] getBluetoothPairedDevices() {
		Log.d(TAG, "Buscando Dispositivos Bluetooth Pareados");
	    
	    BluetoothDevice device = null;
	    
	    this.pairedDevices = btAdapter.getBondedDevices();
	    Log.d(TAG, "Dispositivos Pareados: "+ pairedDevices.size());
	    String[] bluetooths = null;
	    int i=0;
        if(this.pairedDevices.size() > 0)
        {
        	bluetooths = new String[this.pairedDevices.size()];
            for(BluetoothDevice devi : this.pairedDevices)
            {
            	bluetooths[i] = devi.getName().toString();
                i++;
            }
        } else {
        	Toast.makeText(this, "Nao existe dispositivos pareados", Toast.LENGTH_SHORT).show();
        	finish();
        }
        
       return bluetooths;
	}

}
