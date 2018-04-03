package com.example.sholodex;

import android.os.Parcel;
import android.os.Parcelable;

public class IndexCard implements Parcelable {

	Integer id; // null id mean a new form
	String firstName;
	String phoneNumber;
	String email;
	String address;
	String imagePath;

	public IndexCard() {}
	
	public static final Creator<IndexCard> CREATOR = new Creator<IndexCard>() {
        public IndexCard createFromParcel(Parcel in) {
            return new IndexCard(in);
        }
        
        public IndexCard[] newArray(int size) {
            return new IndexCard[size];
        }
	};
        
	@Override
	public int describeContents() {
		return 0;
	}
	
	private IndexCard(Parcel in) {
		int id = in.readInt();
		if (id >= 0) {
			this.id = id;
		}
		firstName = in.readString();
		phoneNumber = in.readString();
		email = in.readString();
		address = in.readString();
		imagePath = in.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if (id != null)
			dest.writeInt(id);
		else
			dest.writeInt(-1);
		dest.writeString(firstName);
        dest.writeString(phoneNumber);
        dest.writeString(email);
        dest.writeString(address);
        dest.writeString(imagePath);
	}
	
	
}
