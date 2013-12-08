package br.net.triangulohackerspace.mariakart;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener {
	private SensorManager sensorManager;
	
	Float inclinacaoVolante;
	ArrayList<Float> leiturasyCoor;
	Integer aceleracaoPorcentagem;
	
	int speedA; //Roda da Esquerda
	int speedB; //Roda da Direita
	boolean parado = false;
	
	TextView xCoor; 
	TextView yCoor; 
	TextView zCoor; 
	TextView aceleracao;
	TextView textOutput;
	SeekBar barraAceleracao;
	ProgressBar speedABar;
	ProgressBar speedBBar;
	
	
	//Bluetooth
	private static final String TAG_BT = "bluetooth1";
	private static final String TAG_SPEED = "speed";
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	
	ScheduledExecutorService scheduleTaskExecutor;
	
	static final int LIST_BLUETOOTH = 1;
	  
	// SPP UUID service 
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static final long INTERVAL_UPDATE = 300; //Tempo de atualizacao do bluetooth
	  
	// MAC-address of Bluetooth module (you must edit this line)
	private String address;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);
		
		//Inicializa Variavel
		leiturasyCoor = new ArrayList<Float>();
		
		setContentView(R.layout.activity_main);
		
		xCoor=(TextView)findViewById(R.id.xcoor); // create X axis object
		yCoor=(TextView)findViewById(R.id.ycoor); // create Y axis object
		zCoor=(TextView)findViewById(R.id.zcoor); // create Z axis object
		aceleracao=(TextView)findViewById(R.id.textAceleracao);
		barraAceleracao=(SeekBar)findViewById(R.id.seekBar1);
		speedABar=(ProgressBar)findViewById(R.id.speedABar);
		speedBBar=(ProgressBar)findViewById(R.id.speedBBar);
		textOutput=(TextView)findViewById(R.id.textOutPut);

		
		sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
		
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		
		/*	More sensor speeds (taken from api docs)
	    SENSOR_DELAY_FASTEST get sensor data as fast as possible
	    SENSOR_DELAY_GAME	rate suitable for games
	 	SENSOR_DELAY_NORMAL	rate (default) suitable for screen orientation changes
	*/
		
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	    checkBTState();
	    
	    textOutput.setText("Bluetooth Desconectado!");

	}

	public void onAccuracyChanged(Sensor sensor,int accuracy){
		
	}
	
	public void onSensorChanged(SensorEvent event){
		
		// check sensor type
		if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
			
			// assign directions
			Float x=event.values[0];
			Float y=event.values[1];
			Float z=event.values[2];
			
			if(leiturasyCoor.isEmpty() || leiturasyCoor.size() < 30 ){
				leiturasyCoor.add(y.floatValue());
			} else {
				//inclinacaoVolante = y.floatValue();
				inclinacaoVolante = calculaMediaInclinacao(y.floatValue());
				
				aceleracaoPorcentagem=barraAceleracao.getProgress();
				
				//xCoor.setText("X: "+x.intValue());
				//zCoor.setText("Z: "+z.intValue());
				yCoor.setText("Y: "+inclinacaoVolante);
				
				aceleracao.setText("Aceleracao: "+ aceleracaoPorcentagem+ " %");
				
				calculaVelocidade();
				
				xCoor.setText("Speed A:"+speedA);
				zCoor.setText("Speed B:"+speedB);
			}
		}
	}


	private Float calculaMediaInclinacao(float floatValue) {
		leiturasyCoor.remove(0);
		leiturasyCoor.add(floatValue);
		Float somatoria = 0F;
		for (Float i : leiturasyCoor) {
			somatoria = somatoria + i;
		}
		return somatoria / leiturasyCoor.size();
	}

	private void calculaVelocidade() {
		
		if(inclinacaoVolante.intValue() >= 1 && inclinacaoVolante.intValue() <= 9){ //Vira para a Direita
			speedA = converteAceleracao();
			speedB = getVelocidadeNaCurva();
		} else if(inclinacaoVolante.intValue() <= -1 && inclinacaoVolante.intValue() >= -9){ //Vira para a Esquerda
			speedA = getVelocidadeNaCurva();
			speedB = converteAceleracao();
		} else if (inclinacaoVolante.intValue() == 0){ //Reto
		 speedA = converteAceleracao();
		 speedB = converteAceleracao();
		}
		
		if(speedB < 0 )
			speedB = 0;
	}
		


	private int getVelocidadeNaCurva() {
		int vel = ((converteAceleracao() * (100-getPorcentagemInclinacao()))/100);
		if(vel < 0)
			vel = 0;
		return vel;
	}

	private int getPorcentagemInclinacao() {
		
		//9 = 100
		//incV = X
		
		// X = (incl*100)/9
		//Log.d(TAG_SPEED, "Inclinacao Volante " + inclinacaoVolante);
		if (inclinacaoVolante < 0){
			//Log.d(TAG_SPEED, "Porcentagem Inclinacao " +Float.valueOf(((Math.abs(inclinacaoVolante)*100)/9)).intValue());
			return (Float.valueOf(((Math.abs(inclinacaoVolante)*100)/9)).intValue()); 
		}else{
			//Log.d(TAG_SPEED, "Porcentagem Inclinacao " + Float.valueOf(((inclinacaoVolante*100)/9)).intValue());
			return (Float.valueOf(((inclinacaoVolante*100)/9)).intValue());
		}
		
		
	}

	private int converteAceleracao() {
		return ((aceleracaoPorcentagem * 255) / 100);
	}
	
	public void mudaDirecao(View view){
			sendData("d:"); //Muda a direcao
	}
	
	

	//Bluetooth
	public void onResume() {
	    super.onResume();
	  
	    Log.d(TAG_BT, "...onResume - try connect...");
	    
	    
	    if(address != null){
	    	conectarBTDevice();
	    }
	    
	    scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
	    
	    if(true){ //Mudar esse if, ele devera fazer a acao caso esteja conectado ao bluetooth
	    	// Essa tarefa rodará a cada meio segundo 
	    	scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
	    	  public void run() {
	    	    processaInformacoes();
	    	    speedABar.setProgress(speedA);
	    	    speedBBar.setProgress(speedB);
	    	  }

			

			private void processaInformacoes() {
				if(speedA > 0 || speedB > 0){
					parado = false;
					String velA = ("000"+Integer.valueOf(speedA).toString());
					String velB = ("000"+Integer.valueOf(speedB).toString());
					
					sendData("b"+velA.substring(velA.length()-3)+velB.substring(velB.length()-3)+":");
				} else 
					if(!parado){
						sendData("h:"); //Freia
						parado = true;
					}
				
			}
	    	}, 0, INTERVAL_UPDATE, TimeUnit.MILLISECONDS);
	    }
	    
	 }
	
	private void conectarBTDevice() {
		
		if(btSocket != null && btSocket.isConnected())
			return;
		
		Log.d(TAG_BT, "Conectando a: "+address);
		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		
		// Two things are needed to make a connection:
		//   A MAC address, which we got above.
		//   A Service ID or UUID.  In this case we are using the
		//     UUID for SPP.
		
		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e1) {
			errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
		}
		
		// Discovery is resource intensive.  Make sure it isn't going on
		// when you attempt to connect and pass your message.
		btAdapter.cancelDiscovery();
		
		// Establish the connection.  This will block until it connects.
		Log.d(TAG_BT, "...Connecting...");
		try {
			btSocket.connect();
			Log.d(TAG_BT, "...Connection ok...");
			Toast.makeText(this, "Bluetooth Conectado", Toast.LENGTH_SHORT).show();
			textOutput.setText("Bluetooth Conectado!");
		} catch (IOException e) {
			textOutput.setText("Bluetooth Desconectado!");
			try {
				btSocket.close();
				Toast.makeText(this, "Bluetooth NAO Conectado", Toast.LENGTH_LONG).show();
				
				Log.d(TAG_BT, "Noo houve conexao com: "+address);
			} catch (IOException e2) {
				errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}
		
		// Create a data stream so we can talk to server.
		Log.d(TAG_BT, "...Create Socket...");
		
		try {
			outStream = btSocket.getOutputStream();
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
		}
	}
	
	 private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
	      if(Build.VERSION.SDK_INT >= 10){
	          try {
	              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
	              return (BluetoothSocket) m.invoke(device, MY_UUID);
	          } catch (Exception e) {
	              Log.e(TAG_BT, "Could not create Insecure RFComm Connection",e);
	          }
	      }
	      return  device.createRfcommSocketToServiceRecord(MY_UUID);
	  }
	 
	 @Override
	  public void onPause() {
	    super.onPause();
	  
		  Log.d(TAG_BT, "...In onPause()...");
		  if(address != null){
			  if (outStream != null) {
				  try {
					  outStream.flush();
				  } catch (IOException e) {
					  errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
				  }
			  }
			  
			  try     {
				  btSocket.close();
			  } catch (IOException e2) {
				  errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
			  }
		  }
		  
		  scheduleTaskExecutor.shutdown();
	  }
	    
	  private void checkBTState() {
	    // Check for Bluetooth support and then check to make sure it is turned on
	    // Emulator doesn't support Bluetooth and will return null
	    if(btAdapter==null) { 
	      errorExit("Fatal Error", "Bluetooth not support");
	    } else {
	      if (btAdapter.isEnabled()) {
	        Log.d(TAG_BT, "...Bluetooth ON...");
	      } else {
	        //Prompt user to turn on Bluetooth
	        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, 1);
	      }
	    }
	  }
	  
	  private void errorExit(String title, String message){
	    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
	    Log.e(TAG_BT,title+" - "+message);
	    finish();
	  }
	  
	  private void sendData(String message) {
	    byte[] msgBuffer = message.getBytes();
	  
	    Log.d(TAG_BT, "...Send data: " + message + "...");
	  
	    try {
	      outStream.write(msgBuffer);
	    } catch (IOException e) {
	      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
	      if (address.equals("00:00:00:00:00:00")) 
	        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
	        msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
	        
	        errorExit("Fatal Error", msg);       
	    }
	  }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_list:
	        	Log.v(TAG_BT, "Chamando lista");
	        	Intent intent = new Intent(this, ListBluetoothActivity.class);
	        	startActivityForResult(intent, LIST_BLUETOOTH);
	        	
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		 Log.i(TAG_BT, "onActivityResult");
         if (requestCode == LIST_BLUETOOTH) {
        	 Log.i(TAG_BT, "LIST_BLUETOOTH");
             if (resultCode == RESULT_OK) {
            	 String nome = data.getStringExtra("BLUETOOTH_MAC");
            	 Log.d(TAG_BT, "Selecionado de volta: "+ nome);
            	 if(nome != null){
            		 if(nome != null && !nome.equals("N��o h�� nenhum conectado")){

            			 Log.d(TAG_BT, "Buscando Dispositivos Bluetooth Pareados");

            			    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            			    Log.d(TAG_BT, "Dispositivos Pareados: "+ pairedDevices.size());
            		        if(pairedDevices.size() > 0)
            		        {
            		            for(BluetoothDevice devi : pairedDevices)
            		            {
            		            	if (devi.getName().toString().equals(nome)) {
            							this.address = devi.getAddress();
            							conectarBTDevice(); 
            							return;
            						};
            		            }
            		        }         		 
                	 }
            	 } 
             }
         }
     }
	
}

