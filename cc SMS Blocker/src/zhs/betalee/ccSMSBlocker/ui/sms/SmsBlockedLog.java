package zhs.betalee.ccSMSBlocker.ui.sms;

import zhs.betalee.ccSMSBlocker.CCBlockerService;
import zhs.betalee.ccSMSBlocker.R;
import zhs.betalee.ccSMSBlocker.database.Constants;
import zhs.betalee.ccSMSBlocker.database.DbAdapter;
import zhs.betalee.ccSMSBlocker.database.UpdataVerDatabase;
import zhs.betalee.ccSMSBlocker.ui.ChoiceWayAddRule;
import zhs.betalee.ccSMSBlocker.ui.CustomMsgViewBinder;
import zhs.betalee.ccSMSBlocker.ui.Donate;
import zhs.betalee.ccSMSBlocker.ui.Settings;
import zhs.betalee.ccSMSBlocker.ui.SwipeDismissListViewTouchListener;
import zhs.betalee.ccSMSBlocker.ui.SwipeSettings;
import zhs.betalee.ccSMSBlocker.ui.TestSMSBlocker;
import zhs.betalee.ccSMSBlocker.util.JsonParse;
import zhs.betalee.ccSMSBlocker.util.MessageUtils;
import zhs.betalee.ccSMSBlocker.util.RecoverAllMsgThread;
import zhs.betalee.ccSMSBlocker.util.SmsManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SmsBlockedLog extends Activity{

//	private ViewFlow viewFlow;
	private ListView blockedmsg_view_listView;
	private DbAdapter mDbAdapter=null;
	private Cursor mBlockedMsgCursor;
	private SimpleCursorAdapter blockedmsgSCAdapter;

    private ProgressDialog builtIndialog;

	private int mAction_Left;
	private int mAction_Right;
	private SwipeDismissListViewTouchListener touchListener=null;
    
	private static Object lock = new Object();
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.smsblockedlog);
		
		mDbAdapter = new DbAdapter(getApplicationContext());
        
		final ImageButton delAllMsgButton=(ImageButton)findViewById(R.id.btn_del_all);
		final ImageButton readAllMsgButton=(ImageButton)findViewById(R.id.btn_read_all);
		final ImageButton recoverAllMsgButton=(ImageButton)findViewById(R.id.btn_recover_all);
		delAllMsgButton.setOnClickListener(new delAllMsgButtonOnClickListener());
		readAllMsgButton.setOnClickListener(new readAllMsgButtonOnClickListener());
		recoverAllMsgButton.setOnClickListener(new recoverAllMsgButtonOnClickListener());
		
		TextView tvmEmpty=(TextView)findViewById(R.id.msg_empty);
		blockedmsg_view_listView = (ListView) findViewById(R.id.blockedmsglist);
		blockedmsg_view_listView.setEmptyView(tvmEmpty);
		blockedmsg_view_listView.setOnItemClickListener(new blockedmsgViewItemClickListener());

		
		if (MessageUtils.readBooleanSharedPreferences(getApplicationContext(), SwipeSettings.SWIPE_ACTION_SMS)) {
			touchListener =new SwipeDismissListViewTouchListener(
					blockedmsg_view_listView,
					new SwipeDismissListViewTouchListener.OnDismissCallback() {
						@Override
						public void onDismiss(ListView listView, int[] reverseSortedPositions,boolean isSwipingRight) {

							for (int position : reverseSortedPositions) {
								if (isSwipingRight) {
									swipeAction(mAction_Right, position);
								}else {
									swipeAction(mAction_Left, position);
								}
							}
							//                            mAdapter.notifyDataSetChanged();
						}
					});
			blockedmsg_view_listView.setOnTouchListener(touchListener);
			// Setting this scroll listener is required to ensure that during ListView scrolling,
			// we don't look for swipes.
			blockedmsg_view_listView.setOnScrollListener(touchListener.makeScrollListener());
		}
		
		try {

			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notificationManager.cancel(R.string.app_name);
		} catch (Exception e) {
			// TODO: handle exception
		}

		MessageUtils.writeUnreadCountSharedPreferences(getApplicationContext(),0);
		long weektime=System.currentTimeMillis()-604800000;
		mDbAdapter.deleteAll(DbAdapter.ALLOW_MESSAGES_DB_TABLE, "timestamp <'"+weektime+"'");
		
		if (!MessageUtils.readAppVerSharedPreferences(getApplicationContext()).equals(getString(R.string.app_ver))) {

			int appVer=Integer.parseInt(MessageUtils.readAppVerSharedPreferences(getApplicationContext()).replaceAll("\\D", ""));
			System.out.println(appVer);
			if (appVer >= 30 && appVer <= 33) {
				mDbAdapter.deleteAll(DbAdapter.DB_TABLE, "rule='����' or rule='��Ʊ' or rule='��Ʊ'");
			}
			if (appVer > 22 && appVer <= 36) {
				mDbAdapter.createOne(".*(http://|www|wap)[\\./#$%+-_?=&a-z\\d]+.*", Constants.CUSTOM_BLOCKED_KEYWORD_REGEXP);
			}
			if (appVer<30) {
		        /*builtIndialog=ProgressDialog.show(SmsBlockedLog.this, // context 
		        	    "", // title 
		        	    "���ڳ�ʼ���������ݿ�...", // message 
		        	    true);

		        UpdataVerDatabase updataVerDatabase=new UpdataVerDatabase(getApplicationContext(),mDbAdapter,handler);
		        updataVerDatabase.start();*/
				new UpdataVerDatabaseTask().execute();
		        
			}else {
				//			���Զ�������
				SharedPreferences  mSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				if (mSharedPreferences.getInt("TestSMSBlocker",0) == 0) {
					startActivity(new Intent(getApplicationContext(), TestSMSBlocker.class));
				}
			}
			
			MessageUtils.writeAppVerSharedPreferences(getApplicationContext(),getString(R.string.app_ver));
	        Toast.makeText(getApplication(),
	        		"��װ���º����ֶ� �����ֻ� һ�Σ�",Toast.LENGTH_LONG).show();
	        Toast.makeText(getApplication(),
	    			"��װ���º����ֶ� �����ֻ� һ�Σ�",Toast.LENGTH_LONG).show();
		}
		String Ecode = MessageUtils.readStringSharedPreferences(getApplicationContext(), "ErrorCode");
		if (Ecode!=null && !Ecode.equals("000")) {
			MessageUtils.updateNotifications(getApplicationContext(),"���æ�Ľ�CC","�������������ң�"+Ecode);
	        Toast.makeText(getApplication(),
	        		"���æ�Ľ�CC���������������ң�"+Ecode,Toast.LENGTH_LONG).show();
			MessageUtils.writeStringSharedPreferences(getApplicationContext(), "ErrorCode", "000");		
		}
		super.onCreate(savedInstanceState);
	}

	
    @SuppressWarnings("deprecation")
	private void refreshBlockedmsgSCAdapter(){
    	/*��Android��API�ĵ��У�Cursor�ķ���requery()����д����This method is deprecated.
    	 * Don't use this. Just request a new cursor, so you can do this asynchronously 
    	 * and update your list view once the new cursor comes back. ����ʾ���Ƿ��յĴ��ڣ�
    	 * ����������󣬻ᵼ����д��ȡ���¼�����Ҳ����requery()��ִ��ʱ�䣩����Ȼ�ֻ������ֲ�����
    	 * ����Ƶ�ʽϵͣ������ݿ������ٵ�ʱ��������������ӣ�������Ȼ���԰�ȫ��ʹ��requery��
    	 * ���Ƕ��ھ��д�������ʱ�����Ǿ���Ҫ�޸�����ĳ���*/
    	mBlockedMsgCursor.requery();
/*    	mBlockedMsgCursor=mDbAdapter.fetchBlockedMSGAll();
		blockedmsgSCAdapter.changeCursor(mBlockedMsgCursor);
		blockedmsgSCAdapter.notifyDataSetChanged();*/
    }
    private void createBlockedmsgView(){

    	mBlockedMsgCursor=mDbAdapter.fetchBlockedMSGAll();
    	blockedmsgSCAdapter=newCustomBlockedMsgSimCurAdap();
    	blockedmsgSCAdapter.setViewBinder(new CustomMsgViewBinder(getApplicationContext()));
    	
		blockedmsg_view_listView.setAdapter(blockedmsgSCAdapter);
//		blockedmsgSCAdapter.notifyDataSetChanged();
    }
    
    private SimpleCursorAdapter newCustomBlockedMsgSimCurAdap(){
    	if (Build.VERSION.SDK_INT >= 11){
    		return new SimpleCursorAdapter(blockedmsg_view_listView.getContext(), R.layout.blockedmsg_list_item_2, mBlockedMsgCursor, 
    				new String[] { "number", "blockedrule","msgbody",DbAdapter.FORMTIME}, 
    				new int[] { R.id.list_item_text1, R.id.list_item_text2,R.id.list_item_text3,R.id.list_item_text4},CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
    	}
		return new SimpleCursorAdapter(blockedmsg_view_listView.getContext(), R.layout.blockedmsg_list_item_2, mBlockedMsgCursor, new String[] { "number", "blockedrule","msgbody",DbAdapter.FORMTIME}, new int[] { R.id.list_item_text1, R.id.list_item_text2,R.id.list_item_text3,R.id.list_item_text4});
    }
	
	
	
	
	
    private class blockedmsgViewItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			// TODO Auto-generated method stub
			TextView numberTextView=(TextView)arg1.findViewById(R.id.list_item_text1);
			numberTextView.setTextColor(0xff000000);
			
			final long rowId=id;
			final int thePosition=position;
			mBlockedMsgCursor.moveToPosition(position);
			//�����Ѷ�
			int status=mBlockedMsgCursor.getInt(5) | 1;
			mDbAdapter.updateMSGStatus(rowId, status);
			//
			handler.sendEmptyMessage(0);
//			refreshBlockedmsgSCAdapter();
			final Context mContext=arg1.getContext();
			//

			new AlertDialog.Builder(mContext)
			.setTitle(mBlockedMsgCursor.getString(1))//���ñ���
			.setMessage(mBlockedMsgCursor.getString(2))//������ʾ��Ϣ
			.setPositiveButton("ɾ��",new DialogInterface.OnClickListener() {//����ȷ���İ���
				public void onClick(DialogInterface dialog, int which) {
					//do something
					mDbAdapter.deleteOne(DbAdapter.BLOCKED_MESSAGES_DB_TABLE,rowId);
//					refreshBlockedmsgView();
					refreshBlockedmsgSCAdapter();
				}
			})
			.setNeutralButton("�ٱ�",new DialogInterface.OnClickListener() {//���ð���
				public void onClick(DialogInterface dialog, int which) {
					//do something
					mBlockedMsgCursor.moveToPosition(thePosition);
					String tempAddString=mBlockedMsgCursor.getString(1);
					String tempMSGString=mBlockedMsgCursor.getString(2);
					String tempRuleString=mBlockedMsgCursor.getString(4);
					MessageUtils.sendBlockMessageToMe(mContext, tempAddString +"] "+
							tempMSGString + tempRuleString );
				}
			})
			.setNegativeButton("�ָ�",new DialogInterface.OnClickListener() {//���ð���
				public void onClick(DialogInterface dialog, int which) {
					//do something
					synchronized (lock) {
						mBlockedMsgCursor.moveToPosition(thePosition);
						String tempAddString=mBlockedMsgCursor.getString(1);
						String tempMSGString=mBlockedMsgCursor.getString(2);
						Long tempTimeString=mBlockedMsgCursor.getLong(3);	
						if (SmsManager.createSms(getApplicationContext(), tempAddString, tempMSGString, tempTimeString)) {
							mDbAdapter.deleteOne(DbAdapter.BLOCKED_MESSAGES_DB_TABLE,rowId);
							Toast.makeText(getApplication(),
									"�ѻָ����ŵ��ռ���",Toast.LENGTH_LONG).show();
							//						refreshBlockedmsgView();
							refreshBlockedmsgSCAdapter();
						}
					}
				}
			})
			//     .setCancelable(false)//���ð����ؼ��Ƿ���Ӧ���أ������ǲ���Ӧ
			.show();
			
		}
    	
    	
    }
	
	
	   private class delAllMsgButtonOnClickListener implements OnClickListener {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final Context mContext=v.getContext();
				new AlertDialog.Builder(mContext)
				.setTitle("ɾ���Ѷ�(��ɫ)")//���ñ���
				.setMessage("ɾ��ȫ���Ѷ�����?")//������ʾ��Ϣ
				.setPositiveButton("ɾ��",new DialogInterface.OnClickListener() {//����ȷ���İ���
					public void onClick(DialogInterface dialog, int which) {
						//do something 1
								mDbAdapter.deleteAll(DbAdapter.BLOCKED_MESSAGES_DB_TABLE,DbAdapter.STATUS + "= 1");
//								refreshBlockedmsgView();
								refreshBlockedmsgSCAdapter();
								Toast.makeText(getApplication(),
										"��ɾ������",Toast.LENGTH_SHORT).show();
						//do something 1
					}
				})
				.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {//���ð���
					public void onClick(DialogInterface dialog, int which) {
						//do something 1
					}
				})
				.show();
			}
	    	
	    }
	
	    private class readAllMsgButtonOnClickListener implements OnClickListener {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				new AlertDialog.Builder(v.getContext())
				.setTitle("���ȫ��")//���ñ���
				.setMessage("�Ƿ���ȫ������Ϊ�Ѷ�?")//������ʾ��Ϣ
				.setPositiveButton("���",new DialogInterface.OnClickListener() {//����ȷ���İ���
					public void onClick(DialogInterface dialog, int which) {
						//do something
						//�����Ѷ�
						mDbAdapter.updateMSGStatus(1, DbAdapter.STATUS + "= 0");
//						refreshBlockedmsgView();
						refreshBlockedmsgSCAdapter();
					}
				})
				.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {//���ð���
					public void onClick(DialogInterface dialog, int which) {
						//do something
						return;
					}
				})
				.show();

			}
	    	
	    }
	    
	    private class recoverAllMsgButtonOnClickListener implements OnClickListener {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
					
				final Context mContext=v.getContext();
				new AlertDialog.Builder(mContext)
				.setTitle("�����ָ�")//���ñ���
				.setMessage("�Ƿ������ָ����ŵ��ռ���?")//������ʾ��Ϣ
				.setPositiveButton("����ָ�",new DialogInterface.OnClickListener() {//����ȷ���İ���
					public void onClick(DialogInterface dialog, int which) {
						//do something 1
						////////////////////////////////
						new AlertDialog.Builder(mContext)
						.setTitle("����")//���ñ���
						.setMessage("�ָ�������˶���?")//������ʾ��Ϣ
						.setPositiveButton("�Ѷ�(��ɫ)",new DialogInterface.OnClickListener() {//����ȷ���İ���
							public void onClick(DialogInterface dialog, int which) {
								//do something 2
								
								//�Ѷ� == 1
								RecoverAllMsgThread recoverAllMsgThread = new RecoverAllMsgThread(getApplicationContext(), mDbAdapter,1,handler);
								recoverAllMsgThread.start();
								builtIndialog=ProgressDialog.show(SmsBlockedLog.this, // context 
						        	    "", // title 
						        	    "���ڡ��ָ����ŵ��ռ���...", // message 
						        	    true);
//								Toast.makeText(getApplication(),
//										"���ڡ��ָ����ŵ��ռ���",Toast.LENGTH_SHORT).show();
							}
						})
						.setNegativeButton("δ��(��ɫ)",new DialogInterface.OnClickListener() {//���ð���
							public void onClick(DialogInterface dialog, int which) {
								//do something 2
								//δ�� == 0
								
								RecoverAllMsgThread recoverAllMsgThread = new RecoverAllMsgThread(getApplicationContext(), mDbAdapter,0,handler);
								recoverAllMsgThread.start();
								builtIndialog=ProgressDialog.show(SmsBlockedLog.this, // context 
						        	    "", // title 
						        	    "���ڡ��ָ����ŵ��ռ���...", // message 
						        	    true);
//								Toast.makeText(getApplication(),
//										"���ڡ��ָ����ŵ��ռ���",Toast.LENGTH_SHORT).show();
								
							}
						})
						.show();
					/////////////////////////
					//do something 1
					}
				})
				.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {//���ð���
					public void onClick(DialogInterface dialog, int which) {
						//do something 1
						return;
					}
				})
				//     .setCancelable(false)//���ð����ؼ��Ƿ���Ӧ���أ������ǲ���Ӧ
				.show();
				
			}
	    }
	

	    private void swipeAction(int mAction,int position){
	    	if (mAction == 1) {
	    		mBlockedMsgCursor.moveToPosition(position);
	    		if (mDbAdapter.deleteOne(DbAdapter.BLOCKED_MESSAGES_DB_TABLE,mBlockedMsgCursor.getLong(0))) {
	    			refreshBlockedmsgSCAdapter();

	    			Toast.makeText(getApplication(),
	    					"��ɾ������",Toast.LENGTH_SHORT).show();
	    		}
	    	}else {
	    		synchronized (lock) {
	    			mBlockedMsgCursor.moveToPosition(position);
	    			String tempAddString=mBlockedMsgCursor.getString(1);
	    			String tempMSGString=mBlockedMsgCursor.getString(2);
	    			Long tempTimeString=mBlockedMsgCursor.getLong(3);	
	    			if (SmsManager.createSms(getApplicationContext(), tempAddString, tempMSGString, tempTimeString)) {
	    				mDbAdapter.deleteOne(DbAdapter.BLOCKED_MESSAGES_DB_TABLE,mBlockedMsgCursor.getLong(0));
	    				Toast.makeText(getApplication(),
	    						"�ѻָ����ŵ��ռ���",Toast.LENGTH_SHORT).show();
	    				refreshBlockedmsgSCAdapter();
	    			}
	    		}
	    	}
	    }
	    
	
	    //����Handler����
	      final Handler handler =new Handler(){
	        	@Override
	        	public void handleMessage(Message msg){
	        		switch (msg.what) {   
	        		case 0:   
	        			//        			refreshBlockedmsgView();  
	        			refreshBlockedmsgSCAdapter();
	        			break;
	        		case 1:   
	        			//        			refreshRulesView();
//	        			refreshRulesSCAdaper();
	        			break;
	        		case 2:   
	        			builtIndialog.dismiss();
	        			Toast.makeText(getApplication(),
	        					"�ָ����ųɹ�",Toast.LENGTH_LONG).show();
	        			refreshBlockedmsgSCAdapter();
	        			break;
	        		case 3:
	        			builtIndialog.dismiss();
	        			Toast.makeText(getApplication(),
	        					"�ָ�����ʧ��",Toast.LENGTH_LONG).show();
	        			break;
	        		case 4:
	        			builtIndialog.dismiss();
//	      			���Զ�������
	        			SharedPreferences  mSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	        			if (mSharedPreferences.getInt("TestSMSBlocker",0) == 0) {
	        				startActivity(new Intent(getApplicationContext(), TestSMSBlocker.class));
	        			}
	        			break;
	        		default:
	        			break;
	        		}
	        	}
	        };


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
//		mBlockedMsgCursor.close();
//		mDbAdapter.close();

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		//�ͷ����ݿ��α�
//		mBlockedMsgCursor.close();
//		mRulesCursor.close();
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		//��ȡ���ݿ��α�
		refreshBlockedmsgSCAdapter();

		super.onResume();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		if (mDbAdapter==null) {
			mDbAdapter = new DbAdapter(getApplicationContext());
		}
		
		createBlockedmsgView();
		String blockedcountString=MessageUtils.readStringSharedPreferences(getApplicationContext(), "blockedcount");
		setTitle(getString(R.string.title_SmsBlockedLog)+"�� �� "+(blockedcountString==null?0:blockedcountString)+" ��");
//		setTitle("è��ʦ Beta 2�� �� "+(blockedcountString==null?0:blockedcountString)+" ��");
		
//		if (blockedcountString!=null&&Integer.parseInt(blockedcountString)>350) {
//			startActivity(new Intent(getApplicationContext(), Donate.class));
//		}
		
		 if (touchListener != null) {
			mAction_Left=MessageUtils.readIntSharedPreferences(getApplicationContext(), SwipeSettings.SWIPE_ACTION_LEFT);
			mAction_Right=MessageUtils.readIntSharedPreferences(getApplicationContext(), SwipeSettings.SWIPE_ACTION_RIGHT);
    		touchListener.setmSwipingLeftColor(mAction_Left==0?0x9999CC00:0x99ff4444);
            touchListener.setmSwipingRightColor(mAction_Right==0?0x9999CC00:0x99ff4444);
		}
		super.onStart();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		mBlockedMsgCursor.close();
		mDbAdapter=null;
		super.onStop();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			startActivity(new Intent(getApplicationContext(), Settings.class));
		}
		return super.onKeyDown(keyCode, event);
	}
	
	class UpdataVerDatabaseTask extends AsyncTask<Void, Void, Void>{
		private SQLiteDatabase mDb;

		@Override
		protected Void doInBackground(Void... params) {
			
//			mDb.beginTransaction(); 
			try {
//				Thread.sleep(350);
				
				//�ڹؼ��� 
				final String[] jsonKeyWord=JsonParse.stringBuilderToArray(JsonParse.jsonToStringBuilder(getApplicationContext(),R.raw.black_keyword_list,true),"WORD");
				 //�׺���
				final String[] jsonWhitePhoneNumber=JsonParse.stringBuilderToArray(JsonParse.jsonToStringBuilder(getApplicationContext(),R.raw.white_num_list,false),null);
	  		
				mDb = mDbAdapter.getmDb();
				mDb.beginTransaction(); 
				
				
				int size=jsonKeyWord.length;
				for (int i=0;i<size;i++) {
					ContentValues values = new ContentValues();
					values.put("rule", jsonKeyWord[i]);
					values.put( "type", Constants.IN_BLOCKED_KEYWORD);
//					values.put(KEY_ENABLED, "1");
					mDb.insert("rules", null, values);
				}
				
			 
				size=jsonWhitePhoneNumber.length;
				for (int i=0;i<size;i++) {
					ContentValues values = new ContentValues();
					values.put("rule", jsonWhitePhoneNumber[i]);
					values.put( "type", Constants.IN_TRUSTED_NUMBER);
//					values.put(KEY_ENABLED, "1");
					mDb.insert("rules", null, values);
				}
							
				ContentValues values = new ContentValues();
				values.put("rule", ".*(http://|www|wap)[\\./#$%+-_?=&a-z\\d]+.*");
				values.put( "type", Constants.CUSTOM_BLOCKED_KEYWORD_REGEXP);
				mDb.insert("rules", null, values);
					
				
				//Here���ݿ����
				mDb.setTransactionSuccessful(); //��������� 
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				mDb.endTransaction(); //Commit
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (builtIndialog.isShowing()) {
				builtIndialog.dismiss();
			}
			Toast.makeText(getApplication(),
	    			"��ʼ����ɣ�",Toast.LENGTH_LONG).show();
			//  			���Զ�������
			SharedPreferences  mSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			if (mSharedPreferences.getInt("TestSMSBlocker",0) == 0) {
				startActivity(new Intent(getApplicationContext(), TestSMSBlocker.class));
			}
			super.onPostExecute(result);
		}


		@Override
		protected void onPreExecute() {
			builtIndialog=ProgressDialog.show(SmsBlockedLog.this, // context 
	        	    "", // title 
	        	    "���ڳ�ʼ���������ݿ�...", // message 
	        	    true,true);
			super.onPreExecute();
		}
		
	}
}
