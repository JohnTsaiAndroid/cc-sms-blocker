package zhs.betalee.ccSMSBlocker.ui;

import zhs.betalee.ccSMSBlocker.R;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class SendSMSToEmail extends InboxRead{

	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		cursor.moveToPosition(position);
		
		try
        {
          Intent localIntent = new Intent("android.intent.action.SENDTO", Uri.parse("mailto:ccsmsblocker@gmail.com,abuse@12321.cn"));
          localIntent.putExtra("android.intent.extra.TEXT", "���ľٱ����츣�����û�!\n 12321.cn���粻����������Ϣ�ٱ���������\n\n"+
        		  cursor.getString(1)+"] "+cursor.getString(2) );
          localIntent.putExtra("android.intent.extra.SUBJECT", "[Report]��ȫ�ƻ�<CC��������>"+getString(R.string.app_ver) );
          startActivity(localIntent);
          finish();
        }
        catch (ActivityNotFoundException localActivityNotFoundException)
        {
        	Toast.makeText(getApplication(), localActivityNotFoundException.getMessage(), 1).show();
        	finish();
        }
		
        
	}

}
