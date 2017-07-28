package com.dotcms.contenttype.model.type;

import com.google.gson.*;
import com.google.gson.reflect.*;
import com.google.gson.stream.*;
import java.io.IOException;
import java.util.Date;
import javax.annotation.Generated;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@code TypeAdapterFactory} that handles all of the immutable types generated under {@code KeyValueContentType}.
 * @see ImmutableKeyValueContentType
 */
@SuppressWarnings("all")
@Generated({"Gsons.generator", "com.dotcms.contenttype.model.type.KeyValueContentType"})
@ParametersAreNonnullByDefault
public final class GsonAdaptersKeyValueContentType implements TypeAdapterFactory {
  @SuppressWarnings({"unchecked", "raw"}) // safe unchecked, types are verified in runtime
  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    if (KeyValueContentTypeTypeAdapter.adapts(type)) {
      return (TypeAdapter<T>) new KeyValueContentTypeTypeAdapter(gson);
    }
    return null;
  }

  @Override
  public String toString() {
    return "GsonAdaptersKeyValueContentType(KeyValueContentType)";
  }

  @SuppressWarnings({"unchecked", "raw"}) // safe unchecked, types are verified in runtime
  private static class KeyValueContentTypeTypeAdapter extends TypeAdapter<KeyValueContentType> {
    public final Date iDateTypeSample = null;
    public final Date modDateTypeSample = null;
    private final TypeAdapter<Date> iDateTypeAdapter;
    private final TypeAdapter<Date> modDateTypeAdapter;

    KeyValueContentTypeTypeAdapter(Gson gson) {
      this.iDateTypeAdapter = gson.getAdapter(TypeToken.get(Date.class));
      this.modDateTypeAdapter = gson.getAdapter(TypeToken.get(Date.class));
    } 

    static boolean adapts(TypeToken<?> type) {
      return KeyValueContentType.class == type.getRawType()
          || ImmutableKeyValueContentType.class == type.getRawType();
    }

    @Override
    public void write(JsonWriter out, KeyValueContentType value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        writeKeyValueContentType(out, value);
      }
    }

    @Override
    public KeyValueContentType read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return readKeyValueContentType(in);
    }

    private void writeKeyValueContentType(JsonWriter out, KeyValueContentType instance)
        throws IOException {
      out.beginObject();
      out.name("multilingualable");
      out.value(instance.multilingualable());
      out.name("name");
      out.value(instance.name());
      String idValue = instance.id();
      if (idValue != null) {
        out.name("id");
        out.value(idValue);
      } else if (out.getSerializeNulls()) {
        out.name("id");
        out.nullValue();
      }
      String descriptionValue = instance.description();
      if (descriptionValue != null) {
        out.name("description");
        out.value(descriptionValue);
      } else if (out.getSerializeNulls()) {
        out.name("description");
        out.nullValue();
      }
      out.name("defaultType");
      out.value(instance.defaultType());
      String detailPageValue = instance.detailPage();
      if (detailPageValue != null) {
        out.name("detailPage");
        out.value(detailPageValue);
      } else if (out.getSerializeNulls()) {
        out.name("detailPage");
        out.nullValue();
      }
      out.name("fixed");
      out.value(instance.fixed());
      out.name("iDate");
      iDateTypeAdapter.write(out, instance.iDate());
      out.name("system");
      out.value(instance.system());
      out.name("versionable");
      out.value(instance.versionable());
      String variableValue = instance.variable();
      if (variableValue != null) {
        out.name("variable");
        out.value(variableValue);
      } else if (out.getSerializeNulls()) {
        out.name("variable");
        out.nullValue();
      }
      String urlMapPatternValue = instance.urlMapPattern();
      if (urlMapPatternValue != null) {
        out.name("urlMapPattern");
        out.value(urlMapPatternValue);
      } else if (out.getSerializeNulls()) {
        out.name("urlMapPattern");
        out.nullValue();
      }
      String publishDateVarValue = instance.publishDateVar();
      if (publishDateVarValue != null) {
        out.name("publishDateVar");
        out.value(publishDateVarValue);
      } else if (out.getSerializeNulls()) {
        out.name("publishDateVar");
        out.nullValue();
      }
      String expireDateVarValue = instance.expireDateVar();
      if (expireDateVarValue != null) {
        out.name("expireDateVar");
        out.value(expireDateVarValue);
      } else if (out.getSerializeNulls()) {
        out.name("expireDateVar");
        out.nullValue();
      }
      String ownerValue = instance.owner();
      if (ownerValue != null) {
        out.name("owner");
        out.value(ownerValue);
      } else if (out.getSerializeNulls()) {
        out.name("owner");
        out.nullValue();
      }
      out.name("modDate");
      modDateTypeAdapter.write(out, instance.modDate());
      out.name("host");
      out.value(instance.host());
      out.name("folder");
      out.value(instance.folder());
      out.endObject();
    }

    private KeyValueContentType readKeyValueContentType(JsonReader in)
        throws IOException {
      ImmutableKeyValueContentType.Builder builder = ImmutableKeyValueContentType.builder();
      in.beginObject();
      while (in.hasNext()) {
        eachAttribute(in, builder);
      }
      in.endObject();
      return builder.build();
    }

    private void eachAttribute(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      String attributeName = in.nextName();
      switch (attributeName.charAt(0)) {
      case 'm':
        if ("multilingualable".equals(attributeName)) {
          readInMultilingualable(in, builder);
          return;
        }
        if ("modDate".equals(attributeName)) {
          readInModDate(in, builder);
          return;
        }
        break;
      case 'n':
        if ("name".equals(attributeName)) {
          readInName(in, builder);
          return;
        }
        break;
      case 'i':
        if ("id".equals(attributeName)) {
          readInId(in, builder);
          return;
        }
        if ("iDate".equals(attributeName)) {
          readInIDate(in, builder);
          return;
        }
        break;
      case 'd':
        if ("description".equals(attributeName)) {
          readInDescription(in, builder);
          return;
        }
        if ("defaultType".equals(attributeName)) {
          readInDefaultType(in, builder);
          return;
        }
        if ("detailPage".equals(attributeName)) {
          readInDetailPage(in, builder);
          return;
        }
        break;
      case 'f':
        if ("fixed".equals(attributeName)) {
          readInFixed(in, builder);
          return;
        }
        if ("folder".equals(attributeName)) {
          readInFolder(in, builder);
          return;
        }
        break;
      case 's':
        if ("system".equals(attributeName)) {
          readInSystem(in, builder);
          return;
        }
        break;
      case 'v':
        if ("versionable".equals(attributeName)) {
          readInVersionable(in, builder);
          return;
        }
        if ("variable".equals(attributeName)) {
          readInVariable(in, builder);
          return;
        }
        break;
      case 'u':
        if ("urlMapPattern".equals(attributeName)) {
          readInUrlMapPattern(in, builder);
          return;
        }
        break;
      case 'p':
        if ("publishDateVar".equals(attributeName)) {
          readInPublishDateVar(in, builder);
          return;
        }
        break;
      case 'e':
        if ("expireDateVar".equals(attributeName)) {
          readInExpireDateVar(in, builder);
          return;
        }
        break;
      case 'o':
        if ("owner".equals(attributeName)) {
          readInOwner(in, builder);
          return;
        }
        break;
      case 'h':
        if ("host".equals(attributeName)) {
          readInHost(in, builder);
          return;
        }
        break;
      default:
      }
      in.skipValue();
    }

    private void readInMultilingualable(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.multilingualable(in.nextBoolean());
    }

    private void readInName(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.name(in.nextString());
    }

    private void readInId(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
      } else {
        builder.id(in.nextString());
      }
    }

    private void readInDescription(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
      } else {
        builder.description(in.nextString());
      }
    }

    private void readInDefaultType(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.defaultType(in.nextBoolean());
    }

    private void readInDetailPage(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        builder.detailPage(null);
      } else {
        builder.detailPage(in.nextString());
      }
    }

    private void readInFixed(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.fixed(in.nextBoolean());
    }

    private void readInIDate(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      Date value = iDateTypeAdapter.read(in);
      builder.iDate(value);
    }

    private void readInSystem(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.system(in.nextBoolean());
    }

    private void readInVersionable(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.versionable(in.nextBoolean());
    }

    private void readInVariable(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
      } else {
        builder.variable(in.nextString());
      }
    }

    private void readInUrlMapPattern(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        builder.urlMapPattern(null);
      } else {
        builder.urlMapPattern(in.nextString());
      }
    }

    private void readInPublishDateVar(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        builder.publishDateVar(null);
      } else {
        builder.publishDateVar(in.nextString());
      }
    }

    private void readInExpireDateVar(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        builder.expireDateVar(null);
      } else {
        builder.expireDateVar(in.nextString());
      }
    }

    private void readInOwner(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        builder.owner(null);
      } else {
        builder.owner(in.nextString());
      }
    }

    private void readInModDate(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      Date value = modDateTypeAdapter.read(in);
      builder.modDate(value);
    }

    private void readInHost(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.host(in.nextString());
    }

    private void readInFolder(JsonReader in, ImmutableKeyValueContentType.Builder builder)
        throws IOException {
      builder.folder(in.nextString());
    }
  }
}