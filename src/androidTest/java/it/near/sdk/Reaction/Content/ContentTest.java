package it.near.sdk.Reaction.Content;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.junit.runner.RunWith;

import it.near.sdk.Reactions.Content.Content;
import it.near.sdk.Reactions.Content.Image;
import it.near.sdk.Reactions.Content.ImageSet;

import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Created by cattaneostefano on 28/02/2017.
 */

@RunWith(AndroidJUnit4.class)
public class ContentTest {

    @Test
    public void contentIsParcelable() {
        Content content = new Content();
        content.setContent("content");
        content.setImages_links(Lists.newArrayList(new ImageSet(), new ImageSet()));
        content.setVideo_link("video_link");
        content.setId("content_id");
        content.setUpdated_at("updated@whatever");
        Parcel parcel = Parcel.obtain();
        content.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Content actual = Content.CREATOR.createFromParcel(parcel);
        assertEquals(content.getContent(), actual.getContent());
        assertThat(content.getImages_links(), containsInAnyOrder(actual.getImages_links().toArray()));
        assertEquals(content.getVideo_link(), actual.getVideo_link());
        assertEquals(content.getId(), actual.getId());
        assertEquals(content.getUpdated_at(), actual.getUpdated_at());
    }

}