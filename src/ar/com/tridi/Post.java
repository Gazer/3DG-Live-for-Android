package ar.com.tridi;


import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.Uri;

public class Post {
	private String avatar;
	private Bitmap bmp;
	public String userName;
	public String title;
	public String postid;
	public String forumId;
	public String userId;
	public String threadId;

	static public Post FromJson(JSONObject post, Bitmap bmp)
	{
		Post p = new Post();
		try {
			p.setAvatar(post.getString("userid"));
			p.setTitle(post.getString("title"));
			p.setUserName(post.getString("username"));
			p.setPostId(post.getString("postid"));
			p.forumId = post.getString("forumid");
			p.userId = post.getString("userid");
			p.threadId = post.getString("threadid");
			p.setBitmap(bmp);
		} catch (Exception e) {
			
		}
		return p;
	}
	
	public Bitmap getBitmap() {
		return bmp;
	}
	public String getAvatar() {
		return avatar;
	}
	
	public void setAvatar(String userid) {
		avatar = "http://foros.3dgames.com.ar/image.php?u=" + userid;
	}
	
	public Uri getUrl()
	{
		return Uri.parse("http://foros.3dgames.com.ar/showthread.php?p="+postid+"#"+postid);
	}
	
	private void setPostId(String id)
	{
		postid = id;
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setBitmap(Bitmap b) {
		bmp = b;
	}
}
