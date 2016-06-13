package it.near.sdk.Reactions.Poll;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import it.near.sdk.MorpheusNear.Resource;

/**
 * @author cattaneostefano
 */
public class Poll extends Resource implements Parcelable{
    @SerializedName("question")
    String question;
    @SerializedName("choice_1")
    String choice_1;
    @SerializedName("choice_2")
    String choice_2;
    @SerializedName("updated_at")
    String updated_at;

    public Poll() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getChoice_1() {
        return choice_1;
    }

    public void setChoice_1(String choice_1) {
        this.choice_1 = choice_1;
    }

    public String getChoice_2() {
        return choice_2;
    }

    public void setChoice_2(String choice_2) {
        this.choice_2 = choice_2;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getQuestion());
        dest.writeString(getChoice_1());
        dest.writeString(getChoice_2());
        dest.writeString(getUpdated_at());
        dest.writeString(getId());
    }

    public static final Parcelable.Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Poll(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Poll[size];
        }
    };

    public Poll(Parcel in){
        setQuestion(in.readString());
        setChoice_1(in.readString());
        setChoice_2(in.readString());
        setUpdated_at(in.readString());
        setId(in.readString());
    }
}
