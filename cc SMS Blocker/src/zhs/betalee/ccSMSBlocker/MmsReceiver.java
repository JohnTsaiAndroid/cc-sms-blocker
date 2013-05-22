package zhs.betalee.ccSMSBlocker;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.damazio.notifier.event.receivers.PduHeaders;
import org.damazio.notifier.event.receivers.PduParser;

import zhs.betalee.ccSMSBlocker.database.Constants;
import zhs.betalee.ccSMSBlocker.database.DbAdapter;
import zhs.betalee.ccSMSBlocker.database.ReadRules;
import zhs.betalee.ccSMSBlocker.ui.Settings;
import zhs.betalee.ccSMSBlocker.util.JsonParse;
import zhs.betalee.ccSMSBlocker.util.MessageUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MmsReceiver extends BroadcastReceiver{
	private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"; 
	private static final String  MMS_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
	private DbAdapter mDbAdapter=null;
//	private Matcher numberMatcher;
	private String formaddress;
	private String MCC;  

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		final Context mContext=context;
		if (!Settings.getBoolean(mContext, "enablemmsblocker")) {
			return;
		}    	

		if (intent.getAction().equals(MMS_RECEIVED_ACTION) && intent.getType().equals("application/vnd.wap.mms-message")) 
		{
			MCC=MessageUtils.fetchMCC(mContext);
			if (mDbAdapter==null) {
				mDbAdapter = new DbAdapter(mContext);
			}
			//����  

			PduParser parser = new PduParser();  

			//				try {  
			PduHeaders headers = parser.parseHeaders(intent.getByteArrayExtra("data"));  
			//                 TransactionId = headers.getTransactionId();  
			if (headers.getMessageType() == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {  
				//�����ȡ  

				
				formaddress = headers.getFrom().getString(); 
				if (formaddress.startsWith(MCC)) {
					formaddress=formaddress.substring(MCC.length());
				}            
				//                final String content_location = headers.getContentLocation();  
				//						if (content_location != null) {  
				//							new Thread() {  
				//								public void run() {  
				//									MmsConnect mmsConnect = new MmsContent(context,content_location,TransactionId);  
				//									try {  
				//										mmsConnect.connect();  
				//									} catch (Exception e) {  
				//										// TODO Auto-generated catch block  
				//										e.printStackTrace();}  
				//								}  
				//							}.start();  
				//
				//						}  
			}  

			//				} catch (InvalidHeaderValueException e) {  
			// TODO Auto-generated catch block  
			//					e.printStackTrace();}  
			/*
			Pattern pattern=Pattern.compile("\\w");
			numberMatcher=pattern.matcher(formaddress);
*/
			//			     ��������ϵ�ˡ�������
			
			final String[] phoneNumbers = ReadRules.getPhoneContacts(mContext,MCC);

			int size=phoneNumbers.length;
			
			try {
				for (int i=0;i<size;i++) {
					if (phoneNumbers[i]==null) {
						continue;
					}
					if (Pattern.matches(phoneNumbers[i], formaddress)) {
						//	    					Toast.makeText(context,
						//	    							phoneNumber,Toast.LENGTH_LONG).show();

						mDbAdapter=null;
						
						return;
					}
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			if (Settings.getBoolean(mContext, "mms_onlycontactwhite")){
				//�׺���
				String[] whitePhoneNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type= "+ Constants.CUSTOM_TRUSTED_NUMBER ,MCC);
				size=whitePhoneNumbers.length;
				try {
					for (int i=0;i<size;i++) {
						if (whitePhoneNumbers[i].contains("?")) {
							whitePhoneNumbers[i]=whitePhoneNumbers[i].replaceAll("\\?", ".");
						}else if (whitePhoneNumbers[i].contains("*")) {
							whitePhoneNumbers[i]=whitePhoneNumbers[i].replaceAll("\\*", ".*");
						}
						//        			Log.e("white", whitePhoneNumbers[i]);
						if (Pattern.matches(whitePhoneNumbers[i], formaddress)) {

							mDbAdapter=null;
						
							return;
						}
					}
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
				whitePhoneNumbers=null;
				blockMessage(mContext, "[��������ϵ�˺Ͱ�����]");
				return;
			}
			

			//�ں���
			final String[] blockNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type = "+ Constants.CUSTOM_BLOCKED_NUMBER ,MCC);

			size=blockNumbers.length;
			StringBuilder tempNum=new StringBuilder();
			for (int i=0;i<size;i++) {
				tempNum.append(blockNumbers[i].replaceAll("\\?", ".").replaceAll("\\*", ".*"));
				try {

					//	        			Log.e("black_num_list", tempNum.toString());
					if (Pattern.matches(tempNum.toString(), formaddress)) {
						blockMessage(mContext, "[����]"+blockNumbers[i]);
						return;
					}
				} catch (RuntimeException e) {

				}
				tempNum.delete(0, tempNum.length());

			}
			//�ں��� end


			//			}  

			mDbAdapter=null;
			
		}  
		////////////////////////////////////////////////////////////////////////////
		//		All End

	}

	private void blockMessage(Context context,String blockString) {
		// TODO Auto-generated method stub
		abortBroadcast();
		Long blockedcount=mDbAdapter.createOne(formaddress, "���źں���[����������]",System.currentTimeMillis(),blockString);

		mDbAdapter=null;
		
		int unreadcount =MessageUtils.readUnreadCountSharedPreferences(context);
		MessageUtils.writeUnreadCountSharedPreferences(context, ++unreadcount);
		MessageUtils.writeStringSharedPreferences(context, "blockedcount", blockedcount.toString());
		MessageUtils.updateNotifications(context,formaddress,"���źں���[����������]");
	}

}
