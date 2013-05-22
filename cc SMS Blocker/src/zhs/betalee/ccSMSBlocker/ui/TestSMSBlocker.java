package zhs.betalee.ccSMSBlocker.ui;

import zhs.betalee.ccSMSBlocker.R;
import zhs.betalee.ccSMSBlocker.database.Constants;
import zhs.betalee.ccSMSBlocker.database.DbAdapter;
import zhs.betalee.ccSMSBlocker.util.MessageUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TestSMSBlocker extends Activity{
	private EditText inpuText;
	private String body;
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
//	private BroadcastReceiver SuccessReceiver;
//	private BroadcastReceiver SendReceiver;
    private ProgressDialog testProgressDialog;
    
	private String inputString;
	private Context mContext;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.testsmsblocker);
		((TextView)findViewById(R.id.TextView_test_sms_blocker)).setMovementMethod(ScrollingMovementMethod.getInstance());
		
		mContext=getApplicationContext();
		
		Button pinfenButton=(Button)findViewById(R.id.btn_test_sms_blocker_pinfen);
		pinfenButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=zhs.betalee.ccSMSBlocker"));
					startActivity(intent); 
				} catch (Exception e) {
					Toast.makeText(getApplication(), "û�ҵ�Google play", 1).show();
				}
			}
		});

		final Button donateButton=(Button)findViewById(R.id.btn_test_sms_blocker_donate);
		donateButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://me.alipay.com/betalee")));
			}
		});
		
		Button goTestButton=(Button)findViewById(R.id.btn_test_sms_blocker);
		
		goTestButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final Context mContext=v.getContext();
				inpuText=new EditText(mContext);
				
				inpuText.setText(MessageUtils.readStringSharedPreferences(getApplicationContext(), "thisphonenumber"));
						//do something 1
		        		new AlertDialog.Builder(mContext)
		        	 	.setTitle("���뱾�ֻ�����")
		        	 	.setMessage("��ȷ���������벻�ڰ�������")//������ʾ��Ϣ
		        	 	.setView(inpuText)
		        	 	.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {//����ȷ���İ���
		    				
							public void onClick(DialogInterface dialog, int which) {
		    					//do something 2
		    					inputString=inpuText.getText().toString().replaceFirst("\\+86", "").replaceAll("\\D", "");
		    					 /** �ֻ����벻Ϊ�� **/  
		    			        if (TextUtils.isEmpty(inputString)) {  
		    			        	Toast.makeText(getApplication(),
		    								"�ֻ����벻Ϊ��",Toast.LENGTH_SHORT).show();
		    			        	return;  
		    			        }  
		    					MessageUtils.writeStringSharedPreferences(getApplicationContext(), "thisphonenumber", inputString);
		    					DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());

		    					if (mDbAdapter.createOne(inputString.toLowerCase(), Constants.CUSTOM_BLOCKED_NUMBER)==-2) {
		    						Toast.makeText(getApplication(),
		    								"�Ѵ�����ͬ����",Toast.LENGTH_SHORT).show();
		    					}else {
		    						Toast.makeText(getApplication(),
		    								"�ɹ���ӹ���",Toast.LENGTH_SHORT).show();
		    					}
/*		    					SmsManager smsManager = SmsManager.getDefault();
		    					body=MessageUtils.formatTimeStampString(mContext, System.currentTimeMillis(),true);
		    					smsManager.sendTextMessage(inputString, null,body , sentPI, deliverPI);   
		    					handler.sendEmptyMessage(1);*/
		    					new SendSMSTask().execute();
		    					//do something 2
		    				}
		    			})
		        	 	.setNegativeButton("ȡ��", null)
		        	 	.show();
		        		//do something 1
			
	
				
			}
		});
		
		
		

        CheckBox cbNeverTest = (CheckBox) findViewById(R.id.cb_nevertest);
        SharedPreferences  mSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        cbNeverTest.setChecked(mSharedPreferences.getInt("TestSMSBlocker",0) == 1);
        cbNeverTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	if (isChecked) {
            		MessageUtils.writeIntSharedPreferences(getApplicationContext(), "TestSMSBlocker", 1);
				}else {
					MessageUtils.writeIntSharedPreferences(getApplicationContext(), "TestSMSBlocker", 0);
				}
            }
        });
        
		super.onCreate(savedInstanceState);
	}


	class SendSMSTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute() {
			testProgressDialog=ProgressDialog.show(TestSMSBlocker.this, // context 
	        	    "", // title 
	        	    "�ȴ����ز��Զ��š�", // message 
	        	    true,true);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			SmsManager smsManager = SmsManager.getDefault();
			body=MessageUtils.formatTimeStampString(getApplicationContext(), System.currentTimeMillis(),true);
			smsManager.sendTextMessage(inputString, null,body , sentPI, deliverPI);   
			
			return null;
		}
		
	
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
	}
	
	
	
	
	//����Handler����
    final Handler handler =new Handler(){
      	@Override
      	public void handleMessage(Message msg){
      		switch (msg.what) {   
      		case 0:   
      			if (testProgressDialog.isShowing()) {
      				testProgressDialog.dismiss();
      			}
 			   Toast.makeText(mContext,  
		    		   "�Ѿ��ɹ����ն���", Toast.LENGTH_SHORT).show();
      			fetchNewSms();
      		default:
      			break;
      		}
      	}
      };
	
      private void fetchNewSms(){
    	  DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());

          final Cursor mBlockedMsgCursor = mDbAdapter.fetchBlockedMSGAll();
          String tempAddString=null;
    	  String tempMSGString=null;
    	  if (mBlockedMsgCursor.moveToFirst()) {
    		  tempAddString=mBlockedMsgCursor.getString(1);
    		  tempMSGString=mBlockedMsgCursor.getString(2);
    		  //				long tempTimeString=mBlockedMsgCursor.getLong(3);
    	  }

    	  if (tempAddString.endsWith(inputString) && tempMSGString.equals(body)) {
    		  new AlertDialog.Builder(TestSMSBlocker.this)
    		  .setTitle("��ϲ")
    		  .setMessage("�ɹ������˲��Զ���!\n\n"+tempMSGString)//������ʾ��Ϣ
    		  .setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {//����ȷ���İ���
    			  public void onClick(DialogInterface dialog, int which) {
    				  SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    				  Editor editor=mSharedPreferences.edit();
    				  editor.putInt("TestSMSBlocker",1);
    				  editor.commit();
    				  DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());

    				  mDbAdapter.deleteAll(DbAdapter.DB_TABLE,  DbAdapter.KEY_NAME + "=" + inputString);

    				  mDbAdapter=null;
    			  }
    		  })
    		  .show();
    	  }else {
//    		  new AlertDialog.Builder(TestSMSBlocker.this)
//    		  .setMessage("CC�Ƿ�ɹ������˲��Զ��ţ�")//������ʾ��Ϣ
//    		  .setPositiveButton("��", new DialogInterface.OnClickListener() {//����ȷ���İ���
//    			  public void onClick(DialogInterface dialog, int which) {
//    				  SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//    				  Editor editor=mSharedPreferences.edit();
//    				  editor.putInt("TestSMSBlocker",1);
//    				  editor.commit();
//    				  DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());
//    				  mDbAdapter.open();
//    				  mDbAdapter.deleteAll(DbAdapter.DB_TABLE,  DbAdapter.KEY_NAME + "=" + inputString);
//    				  mDbAdapter.close();
//    				  mDbAdapter=null;
//    			  }
//    		  })
//    		  .setNegativeButton("��", new DialogInterface.OnClickListener() {
//    			  public void onClick(DialogInterface dialog, int which) {
//    				  //do something 2
    				  new AlertDialog.Builder(TestSMSBlocker.this)
    				  .setTitle("�ܲ��ң�û�����ض���")
    				  .setMessage("�Ƿ��Կ���[��פ��̨����]�������أ�\n�������Լ���Youni��Go���š�Handcent SMS\n\n��������Ҫ�����ֻ�!")//������ʾ��Ϣ
    				  .setPositiveButton("��", new DialogInterface.OnClickListener() {//����ȷ���İ���
    					  public void onClick(DialogInterface dialog, int which) {
    						  //do something 3
    						  SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    						  Editor editor=mSharedPreferences.edit();
    						  editor.putBoolean("enablesmsservice",true);
    						  editor.commit();
    	    				  DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());

    	    				  mDbAdapter.deleteAll(DbAdapter.DB_TABLE,  DbAdapter.KEY_NAME + "=" + inputString);

    	    				  mDbAdapter=null;
    						  //do something 3
    					  }
    				  })
    				  .setNegativeButton("��", new DialogInterface.OnClickListener() {
    					  public void onClick(DialogInterface dialog, int which) {
    						  //do something 3
    						  SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    						  Editor editor=mSharedPreferences.edit();
    						  editor.putBoolean("enablesmsservice",false);
    						  editor.commit();
    	    				  DbAdapter mDbAdapter = new DbAdapter(getApplicationContext());

    	    				  mDbAdapter.deleteAll(DbAdapter.DB_TABLE,  DbAdapter.KEY_NAME + "=" + inputString);

    	    				  mDbAdapter=null;
    						  //do something 3
    					  }
    				  })
    				  .show();


    				  //do something 2
//    			  }
//    		  })
//    		  .show();
    	  }

    
      }
      
      
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
//		unregisterReceiver(SendReceiver);
//		unregisterReceiver(SuccessReceiver);
		super.onDestroy();
	}

	BroadcastReceiver SendReceiver = new BroadcastReceiver() {  
	    @Override  
	    public void onReceive(Context _context, Intent _intent) {  
	        switch (getResultCode()) {  
	        case Activity.RESULT_OK:  
	            Toast.makeText(mContext,  
	        "���ŷ��ͳɹ�", Toast.LENGTH_LONG)  
	        .show();  
	            
	        break;  
	        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:  
	        	Toast.makeText(mContext,  
	    		        "���ŷ���ʧ��", Toast.LENGTH_LONG)  
	    		        .show();  
	        	Toast.makeText(mContext,  
	    		        "���ŷ���ʧ��", Toast.LENGTH_LONG)  
	    		        .show();  
	        	break;  
	        case SmsManager.RESULT_ERROR_RADIO_OFF:  
	        	Toast.makeText(mContext,  
	    		        "û���ź�", Toast.LENGTH_LONG)  
	    		        .show();  
	        break;  
	        case SmsManager.RESULT_ERROR_NULL_PDU:  
	        	Toast.makeText(mContext,  
	    		        "���ŷ���ʧ��", Toast.LENGTH_LONG)  
	    		        .show();  
	        break;  
	        }  
	    }  
	};
	
	BroadcastReceiver SuccessReceiver=new BroadcastReceiver() {  
		   @Override  
		   public void onReceive(Context _context, Intent _intent) {  
		       handler.sendEmptyMessage(0);
		   }  
		};
	private PendingIntent sentPI;
	private PendingIntent deliverPI;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		String SENT_SMS_ACTION = "SENT_SMS_ACTION";  
		Intent sentIntent = new Intent(SENT_SMS_ACTION);  
		sentPI = PendingIntent.getBroadcast(mContext, 0, sentIntent,  
		        0);  
		// register the Broadcast Receivers  
		registerReceiver(SendReceiver,new IntentFilter(SENT_SMS_ACTION)); 
		
		
		String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";  
		// create the deilverIntent parameter  
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);  
		deliverPI = PendingIntent.getBroadcast(mContext, 0,  
		       deliverIntent, 0);  
		registerReceiver(SuccessReceiver, new IntentFilter(DELIVERED_SMS_ACTION));  
		super.onStart();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		unregisterReceiver(SendReceiver);
		unregisterReceiver(SuccessReceiver);
		super.onStop();
	}

}
