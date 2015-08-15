package cc.android.photooverlay;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

public class Form implements Parcelable {

	public enum FormType {
		Mechanical(R.id.rbMechanical),
		Plumbing(R.id.rbPlumbing),
		Process(R.id.rbProcess);
		
		final int radioButtonId;
		
		FormType(int id) {
			this.radioButtonId = id;
		}
	}
	
	Integer id; // null id mean a new form
	Date createDate = new Date();
	Date editDate = new Date();
	String company;
	String customer;
	String location;
	double latitude;
	double longitude;
	String system;
	String plan;
	String detail;
	FormType type = FormType.Mechanical;
	String [] imagePath = new String[3];
	String [] imageMeta = new String[3];
	boolean passed;
	String comments;
	String inspector;
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
	
	private Form(Parcel in) {
		int id = in.readInt();
		if (id >= 0) {
			this.id = id;
		}
		createDate = new Date(in.readLong());
		editDate = new Date(in.readLong());
		company = in.readString();
		customer = in.readString();
		location = in.readString();
		latitude = in.readDouble();
		longitude = in.readDouble();
		in.readStringArray(imagePath);
		in.readStringArray(imageMeta);
		system = in.readString();
		plan = in.readString();
		detail = in.readString();
		try {
			type = FormType.valueOf(in.readString());
		} catch (IllegalArgumentException e) {
			// enum has changed, use default
			e.printStackTrace();
		}
		passed = in.readInt() != 0;
		comments = in.readString();
		inspector = in.readString();
		project = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (id != null)
			dest.writeInt(id);
		else
			dest.writeInt(-1);
		dest.writeLong(createDate.getTime());
		dest.writeLong(editDate.getTime());
		dest.writeString(company);
		dest.writeString(customer);
		dest.writeString(location);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeStringArray(imagePath);
		dest.writeStringArray(imageMeta);
		dest.writeString(system);
		dest.writeString(plan);
		dest.writeString(detail);
		dest.writeString(type.name());
		dest.writeInt(passed ? 1 : 0);
		dest.writeString(comments);
		dest.writeString(inspector);
		dest.writeString(project);
	}
	
	
}
