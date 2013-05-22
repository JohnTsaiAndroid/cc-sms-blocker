package zhs.betalee.ccSMSBlocker;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import zhs.betalee.ccSMSBlocker.database.Constants;
import zhs.betalee.ccSMSBlocker.database.DbAdapter;
import zhs.betalee.ccSMSBlocker.database.ReadRules;
import zhs.betalee.ccSMSBlocker.database.UpdataVerDatabase;
import zhs.betalee.ccSMSBlocker.ui.AdvancedSettings;
import zhs.betalee.ccSMSBlocker.ui.Settings;
import zhs.betalee.ccSMSBlocker.util.JsonParse;
import zhs.betalee.ccSMSBlocker.util.MessageUtils;
import android.R.integer;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.PowerManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.text.format.Time;
import android.util.Log;




/**
 * Handle incoming SMSes.  Just dispatches the work off to a Service.
 */
public class SmsReceiver extends BroadcastReceiver {
	private StringBuilder msgbody=new StringBuilder();
	private String formaddress=null;
	private long fromtime;
	
	private DbAdapter mDbAdapter=null;
//	private ReadRules readRules;
	
//	private Matcher numberMatcher=null;
	private Matcher msgbodyMatcher=null;
	private String addressnumber;
	private String msgbodyString;
	
	private String signaturesString=new String();
	private String MCC;

	
	static final Object mStartingServiceSync = new Object();
    static PowerManager.WakeLock mStartingService=null;
    private static SmsReceiver sInstance=null;
	private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"; 
	private static final String  MMS_RECEIVED_ACTION = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    public static SmsReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new SmsReceiver();
        }
        return sInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent){
    	final Context mContext=context;
//    	����CC���Ź���
    	if (!Settings.getBoolean(mContext, "enablesmsblocker")) {
    		return;
		}   

    	//    	writeError(mContext, "101");
    	beginStartingService(context, intent);

    	
//    	writeError(mContext, "102");
    	if (intent.getAction().equals(SMS_RECEIVED_ACTION)) 
    	{
    		Object[] pdus = (Object[])intent.getExtras().get("pdus");
    		SmsMessage[] messages = new SmsMessage[pdus.length];
    		for (int i = 0; i < pdus.length; i++){
    			messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
    		}
    		
    		msgbody.delete(0, msgbody.length());
    		for (SmsMessage message : messages) 
    		{
    			msgbody.append(message.getMessageBody());
    			formaddress = message.getOriginatingAddress();
    			fromtime=message.getTimestampMillis();
    		}
    	}

    	writeError(mContext, "103");
    	
    	mDbAdapter=null;
    	mDbAdapter = new DbAdapter(mContext);


//    	writeError(mContext, "104");
    	MCC=MessageUtils.fetchMCC(mContext);
//    	System.out.println(MCC);
    	addressnumber=formaddress;
		if (addressnumber.startsWith(MCC)) {
			addressnumber=addressnumber.substring(MCC.length());
		}
		writeError(mContext, "105");
         msgbodyString=msgbody.toString().replaceAll("\\s", "").toLowerCase();
         
//         Log.e("msgbody",msgbody.toString());
//         Log.e("msgbody2",msgbodyString);
//         final long start=System.currentTimeMillis();
        
//         Pattern pattern=Pattern.compile("0");
//         numberMatcher=pattern.matcher(addressnumber);
//         msgbodyMatcher=pattern.matcher(msgbodyString);

         
///////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////
  	
///////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////         
         
//    	��������ϵ�˰�����
		if (Settings.getBoolean(mContext, "onlycontactwhite") || Settings.getBoolean(mContext, "period")) {
			//    		��ϵ��
			writeError(mContext, "106");
			if (allowPhoneContacts(mContext)) {
				return;
			}
//			writeError(mContext, "107");
			//ʱ�ι���			
			if (blockTime(mContext)) {
				return;
			}
//			writeError(mContext, "108");
			if (Settings.getBoolean(mContext, "onlycontactwhite")){
				//        		System.out.println("onlycontactwhite");
				//������
				//�׺���
				if (allowWhiteNumbers(mContext,true)) {
					return;
				}
				//�׹ؼ���
				if (allowWhiteWord(mContext)) {
					return;
				}
				//������end

				blockMessage(mContext,"[��������ϵ�˺Ͱ�����]");
				return;

			}//��������ϵ�˰����� end
		}//    	ʱ�ι��� || ��������ϵ�˰����� end
///////////////////////////////////////////////////////////////////////////////////////////
    	//�Զ���׺���
		writeError(mContext, "109");
		if (allowWhiteNumbers(mContext,false)) {
			return;
		}
		writeError(mContext, "110");
    	//�Զ���׹ؼ���
		if (allowWhiteWord(mContext)) {
			return;
		}
		writeError(mContext, "111");
    	//�Զ���ƥ��λ��
		if (blockConutNumber(mContext, addressnumber)) {
			return;
		}
    	
		writeError(mContext, "112");
    	//�Զ���ں���
		if (blockCustomBlockedNumbers(mContext)) {
			return;
		}
		writeError(mContext, "113");
    	//�Զ���ڹؼ��� 
		if (blockCustomBlockedKeyWords(mContext)) {
			return;
		}
    	
		writeError(mContext, "114");
    	//��ϵ��
    	if (allowPhoneContacts(mContext)) {
			return;
		}
//    	��ϵ�� end
//    	writeError(mContext, "115");
    	//������ʽ 
    	if (blockKeyWordRegexp(mContext)) {
			return;
		}

//    	writeError(mContext, "116");
    	//���ð׺���
    	if (allowInWhiteNumbers(mContext)) {
			return;
		}
    	
    	
//    	writeError(mContext, "117");
    	//�۷Ѻ���
    	if (Pattern.matches("1062.*", addressnumber) || Pattern.matches("1066.*", addressnumber)) {
    		blockMessage(mContext,"[�շ�ҵ��]");
    		return;
    	}
    	//�۷Ѻ��� end   
    	//����թƭ///////////////////
    	final String[] zapianStrings=new String[] {".*�˺�.*",".*�˻�.*",".*��[^\\p{P}]*Ǯ.*",".*Ǯ[^\\p{P}]*��.*",".*��[^\\p{P}]*Ǯ.*",".*Ǯ[^\\p{P}]*��.*",".*��[^\\p{P}]*��.*",
    			".*��[^\\p{P}]*��.*",".*��[^\\p{P}]*��.*",".*��[^\\p{P}]*��.*",".*��[^\\p{P}]*��.*",".*��[^\\p{P}]*��.*",".*����.*����.*",
    			".*����.*����.*",".*��.*����.*��.*",".*����.*����.*",".*֪ͨ.*Υ��.*��ϵ.*",
    			".*����[��\\]\\.\\��]*\\w{0,3}",".*[��\\[].?��[��\\]]*\\w{0,3}"};
    	int size=zapianStrings.length;
    	
    	try {
    		for (int i=0;i<size;i++) {

    			if (Pattern.matches(zapianStrings[i], msgbodyString)&&addressnumber.length()==11) {
    				blockMessage(mContext, "[����թƭ]");
    				return;
    			}
    		}
    	} catch (RuntimeException e) {
    		e.printStackTrace();
    		trycatch(mContext,"003");
    	}
    	
    	//����թƭ end
    	
    	
//    	writeError(mContext, "118");
    	//���úڹؼ��� 
    	if (blockInBlockedKeyWords(mContext)) {
			return;
		}
//    	writeError(mContext, "119");
    	
///////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////
 	
///////////////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////
    	allowMessage(mContext, "[û�й���]");
//    	All End
//    	mContext=null;
//      System.out.println(System.currentTimeMillis()-start);
    }

    private void blockMessage(Context mContext,String blockString) {
		// TODO Auto-generated method stub
//    	writeError(mContext, "120");
    	abortBroadcast();
    	Long blockedcount=mDbAdapter.createOne(formaddress, msgbody.toString(),fromtime,blockString);
    	mDbAdapter=null;


        int unreadcount =MessageUtils.readUnreadCountSharedPreferences(mContext);
        MessageUtils.writeUnreadCountSharedPreferences(mContext, ++unreadcount);
        MessageUtils.updateNotifications(mContext,formaddress.toString(),msgbody.toString());
        MessageUtils.writeStringSharedPreferences(mContext, "blockedcount", blockedcount.toString());
	       
        msgbody.delete(0, msgbody.length());
        writeError(mContext, "000");
        finishBlockSms();
	}

    private void allowMessage(Context mContext,String allowString) {
		// TODO Auto-generated method stub
//    	writeError(mContext, "121");
    	mDbAdapter.createAllowOne(formaddress, fromtime,allowString);
    	mDbAdapter=null;
    	

        msgbody.delete(0, msgbody.length());
        writeError(mContext, "000");
        finishBlockSms();
	}
    
    
    public static String[] concat(String[] a, String[] b) {
    	if (a.length==0) {
			return b;
		}else if (b.length==0) {
			return a;
		}
    	String[] c= new String[a.length+b.length];
    	System.arraycopy(a, 0, c, 0, a.length);
    	System.arraycopy(b, 0, c, a.length, b.length);
    	return c;
    }
    
    
    
    protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
        // If 'privileged' is false, it means that the intent was delivered to the base
        // no-permissions receiver class.  If we get an SMS_RECEIVED message that way, it
        // means someone has tried to spoof the message by delivering it outside the normal
        // permission-checked route, so we just ignore it.
        if (!privileged && intent.getAction().equals(SMS_RECEIVED_ACTION)) {
            return;
        }

        intent.setClass(context, SmsReceiverService.class);
        intent.putExtra("result", getResultCode());
        beginStartingService(context, intent);
    }

    // N.B.: <code>beginStartingService</code> and
    // <code>finishStartingService</code> were copied from
    // <code>com.android.calendar.AlertReceiver</code>.  We should
    // factor them out or, even better, improve the API for starting
    // services under wake locks.
    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     
	 PARTIAL_WAKE_LOCK����Ļ�ͼ��̵�����Ϩ��

     SCREEN_DIM_WAKE_LOCK:��Ļ���ֵ�������Ļ������DIM״̬�������̵�����Ϩ��

     SCREEN_BRIGHT_WAKE_LOCK����Ļ���ֵ��������̵�����Ϩ��

     FULL_WAKE_LOCK����Ļ�ͼ��̵Ʊ��ָ�����ʾ��*/
    private void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
//            System.out.print("mStartingService.acquire");
//            context.startService(intent);
        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
//    public static void finishStartingService(Service service, int startId) {
//        synchronized (mStartingServiceSync) {
//            if (mStartingService != null) {
//                if (service.stopSelfResult(startId)) {
//                    mStartingService.release();
//                }
//            }
//        }
//    }
    private void finishBlockSms() {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
            	mStartingService.release();
//            	System.out.print("mStartingService.release");
            }
        }
    }

    
    /*
    private boolean patternMatches(String regexpstr, Matcher matcher) {
    	Pattern pattern=Pattern.compile(regexpstr);
    	matcher.usePattern(pattern);
    	return matcher.matches();
    }
    private boolean patternFind(String regexpstr, Matcher matcher) {
    	Pattern pattern=Pattern.compile(regexpstr);
    	matcher.usePattern(pattern);
    	if (matcher.find()) {
			return true;
		}
    	matcher.reset();
    	return false;
    }
    */
    private boolean patternFind(String regexpstr, String msgbodyString) {
    	Pattern pattern=Pattern.compile(regexpstr);
    	Matcher matcher = pattern.matcher(msgbodyString);
    	
    	if (matcher.find()) {
			return true;
		}
    	matcher.reset();
    	return false;
    }
    
//    ===================================================
   private boolean allowPhoneContacts(Context mContext){
//	   ��ϵ��
	   String[] phoneNums =ReadRules.getPhoneContacts(mContext,MCC);

    	int size=phoneNums.length;
    	
    	try {
    		for (int i=0;i<size;i++) {
    			if (phoneNums[i]==null) {
    				continue;
    			}
    			if (Pattern.matches(phoneNums[i], addressnumber)) {
    				//					Toast.makeText(context,
    				//							phoneNumber,Toast.LENGTH_LONG).show();
    				// TODO Auto-generated method stub
    				mDbAdapter=null;
    			
    				writeError(mContext, "000");
    				finishBlockSms();
    				return true;
    			}
    		}
    	} catch (RuntimeException e) {
    		e.printStackTrace();
    		trycatch(mContext,"004");
    		return false;
    	}
    	
    	phoneNums=null;
    	return false;
    }
   private boolean blockTime(Context mContext){
	   //ʱ�ι���
	   if (Settings.getBoolean(mContext, "period")) {
		   final Time now = new Time();
		   now.setToNow();
		   final int nowMinuteOfDay=now.hour * 60 + now.minute;
		   final int startMinuteOfDay=Settings.getInt(mContext, "startTimeHour")*60+Settings.getInt(mContext, "startTimeMin");
		   final int endMinuteOfDay=Settings.getInt(mContext, "endTimeHour")*60+Settings.getInt(mContext, "endTimeMin");
		   if (endMinuteOfDay > startMinuteOfDay) {
			   if (nowMinuteOfDay >= startMinuteOfDay && nowMinuteOfDay <= endMinuteOfDay) {
				   blockMessage(mContext, "[ʱ�ι���]");
				   return true;
			   }

		   }else {

			   //    ����ʱ��С�ڿ�ʼʱ��,������
			   if (nowMinuteOfDay >= startMinuteOfDay || nowMinuteOfDay <= endMinuteOfDay) {
				   blockMessage(mContext, "[ʱ�ι���]");
				   return true;
			   }
		   }

	   }
	   return false;
   }
   private boolean allowWhiteNumbers(Context mContext,boolean andInWhiteNumbers){
		//�׺���
		String[] whitePhoneNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type= "+ Constants.CUSTOM_TRUSTED_NUMBER ,MCC);

		if (andInWhiteNumbers && Settings.getBoolean(mContext, "builtinwhitelist")) {
			String[] inWhitePhoneNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type= "+ Constants.IN_TRUSTED_NUMBER,MCC);
			whitePhoneNumbers=concat(whitePhoneNumbers, inWhitePhoneNumbers);
			inWhitePhoneNumbers=null;
		}
		int size=whitePhoneNumbers.length;

		try {
			for (int i=0;i<size;i++) {
				if (whitePhoneNumbers[i].contains("?")) {
					whitePhoneNumbers[i]=whitePhoneNumbers[i].replaceAll("\\?", ".");
				}else if (whitePhoneNumbers[i].contains("*")) {
					whitePhoneNumbers[i]=whitePhoneNumbers[i].replaceAll("\\*", ".*");
				}
				//        			Log.e("white", whitePhoneNumbers[i]);
				if (Pattern.matches(whitePhoneNumbers[i], addressnumber)) {
					allowMessage(mContext, "[�׺���] "+whitePhoneNumbers[i]);

					return true;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			trycatch(mContext,"005");
			return false;
		}

		whitePhoneNumbers=null;
		return false;
   }
   private boolean allowWhiteWord(Context mContext){
		//�׹ؼ���
		String[] whiteWord=ReadRules.getRulesStrings(mDbAdapter,"type= "+ Constants.CUSTOM_TRUSTED_KEYWORD);
		int size=whiteWord.length;

		try {
			for (int i=0;i<size;i++) {
				if (whiteWord[i].contains("*")) {
					whiteWord[i]=whiteWord[i].replaceAll("\\*", ".*");
				}

				if (patternFind(whiteWord[i], msgbodyString)) {
					allowMessage(mContext, "[�Զ��׹ؼ���] "+whiteWord[i]);

					return true;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			trycatch(mContext,"006");
			return false;
		}
		
		whiteWord=null;
		return false;
   }
   private boolean blockConutNumber(Context mContext,String addressnumber){
   	//�Զ���ƥ��λ��
	   String[] conutNumbers=ReadRules.getRulesStrings(mDbAdapter,"type="+ Constants.CUSTOM_BLOCKED_COUNT_NUMBER);
	   int size=conutNumbers.length;

	   final String count=Integer.toString(addressnumber.length());

	   try {	
		   for (int i=0;i<size;i++) {
			   //   		tempCount=Integer.parseInt(conutNumbers[i]);
			   if (count.equals(conutNumbers[i])) {
				   blockMessage(mContext, "[�Զ�ƥ��λ��] "+conutNumbers[i]);
				   return true;
			   }
		   }
	   } catch (RuntimeException e) {
		   e.printStackTrace();
		   trycatch(mContext,"007");
		   return false;
	   }
	   
	   conutNumbers=null;
	   return false;
   }
   private boolean blockCustomBlockedNumbers(Context mContext){
   	//�Զ���ں���
	   String[] phoneNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type= "+ Constants.CUSTOM_BLOCKED_NUMBER ,MCC);

	   int size=phoneNumbers.length;
	   StringBuilder tempNum=new StringBuilder();

	   try {
		   for (int i=0;i<size;i++) {
			   tempNum.append(phoneNumbers[i].replaceAll("\\?", ".").replaceAll("\\*", ".*"));

			   //   			Log.e("black_num_list", tempNum.toString());
			   if (Pattern.matches(tempNum.toString(), addressnumber)) {
				   blockMessage(mContext, "[�Զ��ں���]"+phoneNumbers[i]);
				   return true;
			   }
			   tempNum.delete(0, tempNum.length());

		   }
	   } catch (RuntimeException e) {
		   e.printStackTrace();
		   trycatch(mContext,"008");
		   return false;
	   }
		   
	   
	   phoneNumbers=null;
	   return false;
   }
   private boolean blockCustomBlockedKeyWords(Context mContext){
   	//�Զ���ڹؼ��� 
   	String[] customBKeyWords=ReadRules.getRulesStrings(mDbAdapter,"type="+ Constants.CUSTOM_BLOCKED_KEYWORD);
//   	final String[] keyWords = listKeyWord.toArray(new String[listKeyWord.size()]);
   	int size=customBKeyWords.length;

   	try {
   		for (int i=0;i<size;i++) {

   			if (patternFind(customBKeyWords[i], msgbodyString)) {
   				blockMessage(mContext, "[�Զ��ڴ�] "+customBKeyWords[i]);		
   				return true;
   			}
   		}
   	} catch (RuntimeException e) {
   		e.printStackTrace();
   		trycatch(mContext,"009");
   		return false;
   	}

   	customBKeyWords=null;
   	return false;
   }
   private boolean blockKeyWordRegexp(Context mContext){
   	//������ʽ 
   	String[] regexpKeyWords=ReadRules.getRulesStrings(mDbAdapter,"type='"+ Constants.CUSTOM_BLOCKED_KEYWORD_REGEXP + "'");
   	int size=regexpKeyWords.length;

   	try {	
   		for (int i=0;i<size;i++) {

   			if (Pattern.matches(regexpKeyWords[i], msgbodyString)) {
   				blockMessage(mContext, "[����ʽ] "+regexpKeyWords[i]);
   				return true;
   			}
   		}
   	} catch (RuntimeException e) {
   		//				Log.e("RuntimeException", regexpKeyWords[i]);
   		e.printStackTrace();
   		trycatch(mContext,"010");
   		return false;
   	}

   	regexpKeyWords=null;
   	return false;
   }
   private boolean allowInWhiteNumbers(Context mContext){
	   //���ð׺���
	   if (Settings.getBoolean(mContext, "builtinwhitelist")) {
		   //   		System.out.println("builtinwhitelist");
		   String[] inWhitePhoneNumbers=ReadRules.getRulesNumbers(mDbAdapter,"type= "+ Constants.IN_TRUSTED_NUMBER,MCC);

		   int size=inWhitePhoneNumbers.length;

		   try {
			   for (int i=0;i<size;i++) {
				   if (inWhitePhoneNumbers[i].contains("?")) {
					   inWhitePhoneNumbers[i]=inWhitePhoneNumbers[i].replaceAll("\\?", ".");
				   }else if (inWhitePhoneNumbers[i].contains("*")) {
					   inWhitePhoneNumbers[i]=inWhitePhoneNumbers[i].replaceAll("\\*", ".*");
				   }
				   //   			Log.e("white", whitePhoneNumbers[i]);
				   if (Pattern.matches(inWhitePhoneNumbers[i], addressnumber)) {
					   allowMessage(mContext, "[���ð׺���] "+inWhitePhoneNumbers[i]);
					   return true;
				   }
			   }
		   } catch (RuntimeException e) {
			   e.printStackTrace();
			   trycatch(mContext,"011");
			   return false;
		   }
		   
		   inWhitePhoneNumbers=null;
	   }
	   return false;
   }
   private boolean blockInBlockedKeyWords(Context mContext){
	   //���úڹؼ��� 
	   if (Settings.getBoolean(mContext, "builtinblacklist")) {
		   //   		System.out.println("builtinblacklist");
		   String[] keyWords=ReadRules.getRulesStrings(mDbAdapter,"type='"+ Constants.IN_BLOCKED_KEYWORD +"'");

		   int size=keyWords.length;
		   try {
			   for (int i=0;i<size;i++) {
				   if (patternFind(keyWords[i], msgbodyString)) {
					   blockMessage(mContext, "[���úڴ�] "+keyWords[i]);
					   return true;
				   }
			   }
		   } catch (RuntimeException e) {
			   e.printStackTrace();
			   trycatch(mContext,"012");
			   return false;
		   }
		   keyWords=null;
	   }
	   return false;
   }
   private void writeError(Context context,String errorcode){
	   MessageUtils.writeStringSharedPreferences(context, "ErrorCode", errorcode);
   }
   
   private void trycatch(Context context,String errorcode){
	   MessageUtils.writeStringSharedPreferences(context, "ErrorCode", errorcode);
	   MessageUtils.updateNotifications(context,"���æ�Ľ�CC","�������������ң�"+errorcode);
   }
   
}