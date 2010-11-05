package ar.com.tridi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class Timeline extends ListActivity {
	private ArrayList<Post> posts = null;
	private int lastPostId = 0;
	private PostAdapter adapter;
	private Runnable fetchPosts;
	private String url;
	private Bitmap default_avatar;

	private class PostAdapter extends ArrayAdapter<Post> {
		private ArrayList<Post> items;

		public PostAdapter(Context context, int textViewResourceId, ArrayList<Post> items) {
			super(context, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row, null);
			}
			Post o = items.get(position);
			if (o != null) {
				TextView tt = (TextView) v.findViewById(R.id.toptext);
				TextView bt = (TextView) v.findViewById(R.id.bottomtext);
				ImageView im = (ImageView) v.findViewById(R.id.icon);
				
				if (tt != null) {
					tt.setText(o.getUserName());
				}
				if(bt != null) {
					bt.setText(o.getTitle());
				}
				if (im != null) {
					im.setImageBitmap(o.getBitmap());
				}
			}
			return v;
		}
	}

	Runnable postFetched = new Runnable() {
		public void run() {
			adapter.notifyDataSetChanged();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		url = "http://playground.3dgames.com.ar/3dg-live/stream";

		View view = getListView();
		try {
			default_avatar = BitmapFactory.decodeStream(view.getContext().getAssets().open("avatar.jpg"));
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		posts = new ArrayList<Post>();

		registerForContextMenu(view);
		adapter = new PostAdapter(this, R.layout.row, posts);
		setListAdapter(adapter);
		
		fetchPosts = new Runnable() {
			public void run() {
				while (true) {
					JSONObject post = getHttpJson(url);
					int id = 0;
					try {
						id = post.getInt("postid");
					} catch (Exception e1) {
						id = 0;
					}
					if (id != lastPostId) {
						Post p = Post.FromJson(post, default_avatar);
						posts.add(0, p);
						downloadImage(p.getAvatar(), p);
						if (posts.size() > 20) {
							posts.remove(posts.size()-1);
						}
						runOnUiThread(postFetched);
						lastPostId = id;
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
            }
        };
        Thread thread =  new Thread(null, fetchPosts, "3DGLiveBackground");
        thread.start();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	  super.onCreateContextMenu(menu, v, menuInfo);
	  MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Post post = (Post)posts.get((int)info.id);
		
		switch (item.getItemId()) {
		case R.id.open:
		  Intent myIntent = new Intent(Intent.ACTION_VIEW, post.getUrl());
		  startActivity(myIntent);
		  return true;
		case R.id.follow_forum:
			setURL(post, true, false, false);
			return true;
		case R.id.follow_all:
			setURL(null, false, false, false);
			return true;
		case R.id.follow_user:
			setURL(post, false, true, false);
			return true;
		case R.id.follow_thread:
			setURL(post, false, false, true);
			return false;
	  default:
	    return super.onContextItemSelected(item);
	  }
	}
	
	private void setURL(Post post, boolean fforum, boolean fuser, boolean fthread)
	{
		if (fforum) {
			url = "http://playground.3dgames.com.ar/3dg-live/stream/forum/"+post.forumId;
		} else if (fuser) {
			url = "http://playground.3dgames.com.ar/3dg-live/stream/user/"+post.userId;
		} else if (fthread) {
			url = "http://playground.3dgames.com.ar/3dg-live/stream/thread/"+post.threadId;
		} else {
			url = "http://playground.3dgames.com.ar/3dg-live/stream";
		}
	}
	
	public JSONObject getHttpJson(String url) {
		JSONObject json = null;
		String result = getHttp(url);
		try {
			json = new JSONObject(result);
		} catch (JSONException e) {
			Log.e("JSON", "There was a Json parsing based error", e);
		}
		return json;
	}

	public String getHttp(String url) {
		Log.d("JSON", "getHttp : " + url);
		String result = "";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response;
		
		try {
			response = httpclient.execute(httpget);
			Log.i("JSON", response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				result = convertStreamToString(instream);
				Log.i("JSON", result);
				instream.close();
			}
		} catch (ClientProtocolException e) {
			Log.e("JSON", "There was a protocol based error", e);
		} catch (IOException e) {
			Log.e("JSON", "There was an IO Stream related error", e);
		} catch (Exception e) {
			Log.e("JSON", "Unknown Error", e);
		}
		return result;
	}

	private static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

    private void downloadImage(String urlStr, Object targetObj) {
    	//progressDialog = ProgressDialog.show(this, "", "Fetching Image...");
    	final String url = urlStr;
    	final Object obj = targetObj;

    	new Thread() {
    		public void run() {
    			InputStream in = null;
    			Message msg = Message.obtain();
    			msg.what = 1;
    			msg.obj = obj;
    			try {
    				in = openHttpConnection(url);
    				Bitmap bitmap = BitmapFactory.decodeStream(in);
    				Bundle b = new Bundle();
    				b.putParcelable("bitmap", bitmap);
    				msg.setData(b);
    				in.close();
                } catch (Exception e1) {
                	e1.printStackTrace();
                }

                messageHandler.sendMessage(msg);
            }
        }.start();
    }

    //ProgressDialog progressDialog;
    private Handler messageHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		switch (msg.what) {
    		case 1:
    			try {
    				Post post = (Post)msg.obj;
    				Bitmap bitmap = (Bitmap)(msg.getData().getParcelable("bitmap"));
    				bitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false);
    				post.setBitmap(bitmap);
    				runOnUiThread(postFetched);
    			} catch (Exception e){
    				// Ignore errors
    			}
            break;
    		}
    		//progressDialog.dismiss();
        }
    };

    private InputStream openHttpConnection(String urlStr)
    {
        InputStream in = null;
        int resCode = -1;

        try {
        	URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException ("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection)urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream(); 
            } 
        } catch (MalformedURLException e) {
        	e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        	// Ignore other errors
        }

        return in;
    }
}