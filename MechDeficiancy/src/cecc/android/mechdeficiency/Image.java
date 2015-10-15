package cecc.android.mechdeficiency;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Entity mapped to table "IMAGE".
 */
public class Image implements Parcelable {

	long formId;
    int index;
    String path;
    String data;

    public Image() {
    }

    public Image(String path, String data, int index, long formId) {
        this.path = path;
        this.data = data;
        this.index = index;
        this.formId = formId;
    }

	public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }
        
        public Image[] newArray(int size) {
            return new Image[size];
        }
	};
        
	@Override
	public int describeContents() {
		return 0;
	}
	
	private Image(Parcel in) {
		formId = in.readLong();
		index = in.readInt();
		path = in.readString();
		data = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(formId);
		dest.writeInt(index);
		dest.writeString(path);
		dest.writeString(data);
	}
	
	@Override
	public String toString() {
		return path + " (" + index + ") form=" + formId + " " + data;
	}
}
