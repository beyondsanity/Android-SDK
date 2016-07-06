package it.near.sdk.MorpheusNear;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Morpheus is a library to map JSON with the json:api specification format.
 * (http://jsonapi.org/).
 *
 * Feel free to contribute on github. (//TODO insert new link here)
 *
 * Example
 * <pre>
 * {@code
 *  Morpheus morpheus = new Morpheus();
 *  JSONAPIObject jsonapiObject = morpheus.parse(YOUR-JSON-STRING);
 * }
 * </pre>
 */
public class Morpheus {
  private Mapper mapper;
  private Factory factory;

  public Morpheus() {
    mapper = new Mapper();
    factory = new Factory();
    factory.setMapper(mapper);
  }

  public Morpheus(AttributeMapper attributeMapper) {
    mapper = new Mapper(new Deserializer(), attributeMapper);
    factory = new Factory();
    factory.setMapper(mapper);
  }

  public Factory getFactory() {
    return factory;
  }

  /**
   * Will return you an {@link JSONAPIObject} with parsed objects, links, relations and includes.
   *
   * @param jsonString Your json:api formated string.
   * @return A {@link JSONAPIObject}.
   * @throws JSONException or NotExtendingResourceException
   */
  public JSONAPIObject parse(String jsonString) throws Exception {
    JSONObject jsonObject = null;
    try {
      jsonObject = new JSONObject(jsonString);
    } catch (JSONException e) {
      throw e;
    }

    return parseFromJSONObject(jsonObject);
  }

  /**
   * Parse and map all the top level members.
   */
  private JSONAPIObject parseFromJSONObject(JSONObject jsonObject) throws Exception {
    JSONAPIObject jsonApiObject = new JSONAPIObject();

    //included
    try {
      JSONArray includedArray = jsonObject.getJSONArray("included");
      jsonApiObject.setIncluded(factory.newObjectFromJSONArray(includedArray, null));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain included");
    }

    //data array
    JSONArray dataArray = null;
    try {
      dataArray = jsonObject.getJSONArray("data");
      jsonApiObject.setResources(factory.newObjectFromJSONArray(dataArray, jsonApiObject.getIncluded()));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain data array");
    }

    //data object
    JSONObject dataObject = null;
    try {
      dataObject = jsonObject.getJSONObject("data");
      jsonApiObject.setResource(factory.newObjectFromJSONObject(dataObject, jsonApiObject.getIncluded()));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain data object");
    }

    //link object
    JSONObject linkObject = null;
    try {
      linkObject = jsonObject.getJSONObject("links");
      jsonApiObject.setLinks(mapper.mapLinks(linkObject));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain links object");
    }

    //meta object
    JSONObject metaObject = null;
    try {
      metaObject = jsonObject.getJSONObject("meta");
      jsonApiObject.setMeta(mapper.getAttributeMapper().createMapFromJSONObject(metaObject));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain meta object");
    }

    JSONArray errorArray = null;
    try {
      errorArray = jsonObject.getJSONArray("errors");
      jsonApiObject.setErrors(mapper.mapErrors(errorArray));
    } catch (JSONException e) {
      Logger.debug("JSON does not contain errors object");
    }

    return jsonApiObject;
  }
}
