package dc.maitetsu.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Park Hyo Jun
 * @since 2017-04-22
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GalleryInfo implements Serializable {
  private String galleryName;
  private String galleryCode;
}
