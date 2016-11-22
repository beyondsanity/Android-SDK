package it.near.sdk.Reactions.Feedback;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import it.near.sdk.MorpheusNear.Resource;
import it.near.sdk.Recipes.Models.ReactionBundle;

/**
 * Created by cattaneostefano on 11/10/2016.
 */

public class Feedback extends ReactionBundle implements Parcelable{

    @SerializedName("question")
    String question;

    String recipeId;

    public Feedback() {
    }

    protected Feedback(Parcel in) {
        question = in.readString();
        setRecipeId(in.readString());
        setId(in.readString());
    }

    public static final Creator<Feedback> CREATOR = new Creator<Feedback>() {
        @Override
        public Feedback createFromParcel(Parcel in) {
            return new Feedback(in);
        }

        @Override
        public Feedback[] newArray(int size) {
            return new Feedback[size];
        }
    };

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeString(getRecipeId());
        dest.writeString(getId());
    }
}
