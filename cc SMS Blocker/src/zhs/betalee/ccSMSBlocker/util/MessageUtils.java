package zhs.betalee.ccSMSBlocker.util;


import zhs.betalee.ccSMSBlocker.R;
import zhs.betalee.ccSMSBlocker.ui.Main;
import zhs.betalee.ccSMSBlocker.ui.Settings;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.widget.Toast;

public class MessageUtils {
	
	public static void updateNotifications(Context context,String body){

		if (!Settings.getBoolean(context, "enablenotification")) {	
			return;
		}

		//����֪ͨ����Ϣ�������� 
		NotificationManager mNotificationManager;
		Intent  mIntent;
		PendingIntent mPendingIntent;
		//����Notification����
		Notification  mNotification;

		//��ʼ��NotificationManager���� 
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mIntent=new Intent(context, Main.class);
		/** ���� */

		//���֪ͨʱת������ 
		//Intent intent = new Intent(this, this.getClass());
		//intent.addCategory(WINDOW_SERVICE);
		//��Ҫ�����õ��֪ͨʱ��ʾ���ݵ��� 
		mPendingIntent = PendingIntent.getActivity(context, 0, mIntent, 0); //���ת����������m_Intent();
		//����Notification���� 
		mNotification = new Notification(); 
		//����֪ͨ��״̬����ʾ��ͼ�� 
		mNotification.icon = R.drawable.noti;
		//�����ǵ��֪ͨʱ��ʾ������ 
		mNotification.tickerText = "�ѹ��˶���:"+body; 
		//֪ͨʱ����Ĭ�ϵ����� 
		//		  mNotification.defaults = Notification.DEFAULT_SOUND;

		//����֪ͨ��ʾ�Ĳ��� 
		mNotification.setLatestEventInfo(context, context.getString(R.string.app_name), body, mPendingIntent); 
		//�������Ϊִ�����֪ͨ 
		mNotificationManager.notify(R.string.app_name, mNotification); 

		/** ȡ�� */

		//		  mNotificationManager.cancelAll();

	}
	
	public static void sendBlockMessageToMe(Context context,String msg){
		try
        {
          Intent localIntent = new Intent("android.intent.action.SENDTO", Uri.parse("mailto:godwasdog@gmail.com"));
          localIntent.putExtra("android.intent.extra.TEXT", "���ľٱ����츣�����û�!\n\n"+msg );
          localIntent.putExtra("android.intent.extra.SUBJECT", "[Report]��л���ľٱ�" );
          context.startActivity(localIntent);
        }
        catch (ActivityNotFoundException localActivityNotFoundException)
        {
          Toast.makeText(context, localActivityNotFoundException.getMessage(), 1).show();
        }
	}
 
	
	public static String formatTimeStampString(Context context, long when, boolean fullFormat) {
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();
        // Basic settings for formatDateTime() we want for all cases.
        int format_flags = DateUtils.FORMAT_NO_NOON_MIDNIGHT |
                           DateUtils.FORMAT_ABBREV_ALL |
                           DateUtils.FORMAT_CAP_AMPM;
        // If the message is from a different year, show the date and year.
        if (then.year != now.year) {
            format_flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            // If it is from a different day than today, show only the date.
            format_flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            // Otherwise, if the message is from today, show the time.
            format_flags |= DateUtils.FORMAT_SHOW_TIME;
        }
        // If the caller has asked for full details, make sure to show the date
        // and time no matter what we've determined above (but still make showing
        // the year only happen if it is a different year from today).
        if (fullFormat) {
            format_flags |= (DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        }
        return DateUtils.formatDateTime(context, when, format_flags);
    }
}
