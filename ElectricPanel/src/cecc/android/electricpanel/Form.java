package cecc.android.electricpanel;

import java.util.*;

import android.os.Parcel;
import android.os.Parcelable;

public class Form implements Parcelable {

	Long id; // null id mean a new form
	Date createDate = new Date();
	Date editDate = new Date();
	String customer;
	String location;
	double latitude;
	double longitude;
	String plan;
	String type;
	final List<Image> images = new ArrayList<>();
	boolean passed;
	boolean torqued;
	String comments;
	String representative;
	String project;

	public Form() {}
	
	public static final Parcelable.Creator<Form> CREATOR = new Parcelable.Creator<Form>() {
        public Form createFromParcel(Parcel in) {
            return new Form(in);
        }
        
        public Form[] newArray(int size) {
            return new Form[size];
        }
	};
        
	@Override
	public int describeContents() {
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	private Form(Parcel in) {
		long id = in.readLong();
		if (id >= 0) {
			this.id = id;
		}
		createDate = new Date(in.readLong());
		editDate = new Date(in.readLong());
		customer = in.readString();
		location = in.readString();
		latitude = in.readDouble();
		longitude = in.readDouble();
		images.clear();
		images.addAll(in.readArrayList(Image.class.getClassLoader())); 
		plan = in.readString();
		type = in.readString();
		passed = in.readInt() != 0;
		torqued = in.readInt() != 0;
		comments = in.readString();
		representative = in.readString();
		project = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (id != null)
			dest.writeLong(id);
		else
			dest.writeLong(-1);
		dest.writeLong(createDate.getTime());
		dest.writeLong(editDate.getTime());
		dest.writeString(customer);
		dest.writeString(location);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeList(images);
		dest.writeString(plan);
		dest.writeString(type);
		dest.writeInt(passed ? 1 : 0);
		dest.writeInt(torqued ? 1 : 0);
		dest.writeString(comments);
		dest.writeString(representative);
		dest.writeString(project);
	}
	
	public Image getImageForIndex(int index) {
		for (Image i : images) {
			if (i.index == index)
				return i;
		}
		return null;
	}
	
}
