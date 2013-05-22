package zhs.betalee.ccSMSBlocker.database;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import zhs.betalee.ccSMSBlocker.util.JsonParse;

import android.R;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.text.TextUtils;
import android.util.Log;

public class ReadRules {
	//	private static final String[] PHONES_PROJECTION = new String[] {  
	//	       Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID,Phone.CONTACT_ID };  
	private static final String[] PHONES_PROJECTION = new String[] {Phone.NUMBER};
	//	/**��ϵ����ʾ����**/  
	//    private static final int PHONES_DISPLAY_NAME_INDEX = 0;  
	//      
	//    /**�绰����**/  
	//    private static final int PHONES_NUMBER_INDEX = 1;  
	//      
	//    /**ͷ��ID**/  
	//    private static final int PHONES_PHOTO_ID_INDEX = 2;  
	//     
	//    /**��ϵ�˵�ID**/  
	//    private static final int PHONES_CONTACT_ID_INDEX = 3;  
	//	


	/**�õ��ֻ�ͨѶ¼��ϵ����Ϣ
	 * @return **/
	public static String[] getPhoneContacts(Context context,String MCC) {
		//  	ContentResolver resolver = mContext.getContentResolver();
//		ArrayList<String> listPhoneNumber = new ArrayList<String>();
		Context mContext=context;

		int MCClength = MCC.length();
		// ��ȡ�ֻ���ϵ��
		Cursor phoneCursor=null ;
		try {
			phoneCursor = mContext.getContentResolver().query(Phone.CONTENT_URI,PHONES_PROJECTION, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] listPhoneNumber = new String[phoneCursor.getCount()];
		int i=0;
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {

				//�õ��ֻ�����
				String phoneNumber = phoneCursor.getString(0);

				//���ֻ�����Ϊ�յĻ���Ϊ���ֶ� ������ǰѭ��
				if (TextUtils.isEmpty(phoneNumber))
					continue;
				
				if (phoneNumber.startsWith(MCC)) {
		        	phoneNumber=phoneNumber.substring(MCClength);
				}				
				phoneNumber=phoneNumber.replaceAll("\\D", "");

								
				//�õ���ϵ������
				//		String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);

				//�õ���ϵ��ID
				//		Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

				//�õ���ϵ��ͷ��ID
				//		Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);

				//�õ���ϵ��ͷ��Bitamp
				//		Bitmap contactPhoto = null;

				//photoid ����0 ��ʾ��ϵ����ͷ�� ���û�и���������ͷ�������һ��Ĭ�ϵ�
				//		if(photoid > 0 ) {
				//		    Uri uri =ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,contactid);
				//		    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
				//		    contactPhoto = BitmapFactory.decodeStream(input);
				//		}else {
				//		    contactPhoto = BitmapFactory.decodeResource(getResources(), R.drawable.contact_photo);
				//		}

				//		mContactsName.add(contactName);
				//		mContactsNumber.add(phoneNumber);
				//		mContactsPhonto.add(contactPhoto);
				
				listPhoneNumber[i]=phoneNumber;
				i++;
			}


			//  		resolver=null;
		}
		phoneCursor.close();
		phoneCursor=null;
		return listPhoneNumber;
	}
	// ͨ��address�ֻ��Ź���Contacts��ϵ�˵���ʾ����  
	public static String getPeopleNameFromPerson(Context context,String address){  
        if(address == null || address == ""){  
            return "( no address )\n";  
        }  
          
        String strPerson = null;  
        String[] projection = new String[] {Phone.DISPLAY_NAME, Phone.NUMBER};  
          
        Uri uri_Person = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, address);  // address �ֻ��Ź���  
        Cursor cursor = context.getContentResolver().query(uri_Person, projection, null, null, null);  
          
        if(cursor.moveToFirst()){  
            int index_PeopleName = cursor.getColumnIndex(Phone.DISPLAY_NAME);  
            String strPeopleName = cursor.getString(index_PeopleName);  
            strPerson = strPeopleName;  
        }  
        cursor.close();  
        cursor=null; 
        return strPerson;  
    }  
	public static boolean contactExists(Context context, String number) {
		/// number is the phone number
		Uri lookupUri = Uri.withAppendedPath(
				PhoneLookup.CONTENT_FILTER_URI, 
				Uri.encode(number));
		String[] mPhoneNumberProjection = { PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME };
		Cursor cur = context.getContentResolver().query(lookupUri,mPhoneNumberProjection, null, null, null);
		try {
			if (cur.moveToFirst()) {
				return true;
			}
		} finally {
			if (cur != null){
				cur.close();
				cur=null;
			}
		}
		return false;
	}
	
	
	
	public static String[] getRulesNumbers(DbAdapter dbAdapter,String selection,String MCC){
//		ArrayList<String> listPhoneNumber = new ArrayList<String>();
		DbAdapter mDbAdapter=dbAdapter;
		dbAdapter=null;
		
		int MCClength = MCC.length();
		// ��ȡ
		Cursor phoneCursor = mDbAdapter.fetchAllRulesType(selection);
		final int cursorSiez=phoneCursor.getCount();
		String[] listPhoneNumber=new String[cursorSiez];
		int i=0;
		if (phoneCursor != null) {
			while (phoneCursor.moveToNext()) {

				//�õ��ֻ�����
				String phoneNumber = phoneCursor.getString(1);
				
				//���ֻ�����Ϊ�յĻ���Ϊ���ֶ� ������ǰѭ��
				if (TextUtils.isEmpty(phoneNumber))
					continue;
				
				
				if (phoneNumber.startsWith(MCC)) {
		        	phoneNumber=phoneNumber.substring(MCClength);
				}
				phoneNumber=phoneNumber.replaceAll("[^\\d?*]", "");

//		        	else if (phoneNumber.startsWith("85") && phoneNumber.length() > 12) {
//					phoneNumber=phoneNumber.replaceFirst("85\\.","");
//				}else if (condition) {
//					
//				}
//				if (phoneNumber.contains("-") || phoneNumber.contains(" ")) {
//					Log.e("-", phoneNumber);
//					phoneNumber=phoneNumber.replaceAll("[^\\d?*]", "");
//				}


				listPhoneNumber[i]=phoneNumber;
//				System.out.println(listPhoneNumber[i]);
				i++;
			}

		}

		phoneCursor.close();
		phoneCursor=null;
		return listPhoneNumber;

	}
	
	public static String[] getRulesStrings(DbAdapter dbAdapter,String selection){
		DbAdapter mDbAdapter=dbAdapter;
		dbAdapter=null;
		// ��ȡ
		Cursor wordCursor = mDbAdapter.fetchAllRulesType(selection);
		final int cursorSiez=wordCursor.getCount();
		String[] keyWoeds=new String[cursorSiez];
		int i=0;
		if (wordCursor != null) {
			while (wordCursor.moveToNext()) {

				
				String keyWoed = wordCursor.getString(1);
				
				//���ֻ�����Ϊ�յĻ���Ϊ���ֶ� ������ǰѭ��
				if (TextUtils.isEmpty(keyWoed))
					continue;

				keyWoeds[i]=keyWoed;
//				System.out.println(keyWoeds[i]);
				i++;
			}


		}
		wordCursor.close();
		wordCursor=null;
		return keyWoeds;

	}
	
	


	
	
}
