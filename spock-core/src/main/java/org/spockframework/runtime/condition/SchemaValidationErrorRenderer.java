/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spockframework.runtime.condition;

import java.util.*;

import cz.cvut.fit.schemaforg.api.ValidationError;

public class SchemaValidationErrorRenderer {


  public StringBuilder render(Object value, Object validationResult) {
    List<Line> buffer = render(value, validationResult, new Path(), new LinkedList<Line>());
    int maxLineLength = getMaxLineLength(buffer);
    StringBuilder sb = new StringBuilder();
    int idx = 0;
    for (Line segment : buffer) {
      sb.append(segment.getContent());
      if (segment.getErrorMsg() != null) {
        char[] padding = new char[maxLineLength - getLastLineLength(sb)];
        Arrays.fill(padding, '-');
        sb.append(padding);
        // TODO
        sb.append(" " + segment.getErrorMsg().toString());
      }
    }
    return sb;
  }

  protected List<Line> render(Object value, Object validationResult, Path currentPath, List<Line> buffer) {
    Object currValue = currentPath.resolve(value);
    ValidationError currError = resolveErrorOnPath(validationResult, currentPath);

    if (currValue instanceof List) {
      return renderList(value, validationResult, currentPath, buffer);
    } else if (currValue instanceof Map) {
      return renderMap((Map) value, validationResult, currentPath, buffer);
    } else if (currValue instanceof String) {
      appendLine(buffer, indent(currentPath.length()) + "\"" + currValue + "\"", currError);
      return buffer;
    } else if (currValue instanceof Number) {
      appendLine(buffer, indent(currentPath.length()) + "\"" + currValue.toString() + "\"", currError);
      return buffer;
    } else {
      appendLine(buffer, indent(currentPath.length()) + javaLangObjectToString(currValue), currError);
      return buffer;
    }
  }

  protected List<Line> renderList(Object value, Object validationResult, Path currentPath, List<Line> buffer) {
    List currValue = (List) currentPath.resolve(value);
    ValidationError currError = resolveErrorOnPath(validationResult, currentPath);
    appendLine(buffer, indent(currentPath.length()) + "[", currError);
    for (int idx = 0; idx < currValue.size(); idx++) {
      render(value, validationResult, currentPath.push(idx), buffer);
    }
    appendLine(buffer, indent(currentPath.length()) + "]", currError);
    return buffer;
  }

  public List<Line> renderMap(Map value, Object validationResult, Path currentPath, List<Line> buffer) {
    ValidationError currError = resolveErrorOnPath(validationResult, currentPath);
    appendLine(buffer, indent(currentPath.length()) + "[", currError);
    for (Object key : value.keySet()) {
      appendText(buffer, indent(currentPath.length()) + key.toString() + ": ", null);
      render(value, validationResult, currentPath.push(key), buffer);
    }
    appendLine(buffer, indent(currentPath.length()) + "]", null);
    return buffer;
  }


  private ValidationError resolveErrorOnPath(Object validationResult, Path path) {
    Object res = path.resolve(validationResult);
    if (res instanceof ValidationError) {
      return (ValidationError) res;
    }
    return null;
  }

  private int getMaxLineLength(List<Line> buffer) {
    StringBuilder sb = new StringBuilder();

    for (Line segment : buffer) {
      sb.append(segment.getContent());
    }
    int max = 0;
    for (String line : sb.toString().split("\n")) {
      max = Math.max(line.length(), max);
    }
    return max;
  }

  private int getLastLineLength(StringBuilder sb) {
    // TODO fix this
    int lastEndLine = sb.lastIndexOf("\n");
    int startOfLine = sb.lastIndexOf("\n", lastEndLine - 1);
    return lastEndLine - startOfLine - 1;
  }

  private void appendLine(List buffer, String line, ValidationError error) {
    appendText(buffer, line + "\n", error);
  }

  private void appendText(List buffer, String text, ValidationError error) {
    buffer.add(new Line(text, error));
  }

  private String indent(int indent) {
    char[] buf = new char[indent];
    Arrays.fill(buf, ' ');
    return new String(buf);
  }

  private String javaLangObjectToString(Object value) {
    String hash = Integer.toHexString(System.identityHashCode(value));
    return value.getClass().getName() + "@" + hash;
  }

  class Line {
    private String content;
    private ValidationError errorMsg;

    Line(String content, ValidationError errorMsg) {
      this.content = content;
      this.errorMsg = errorMsg;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public ValidationError getErrorMsg() {
      return errorMsg;
    }

    public void setErrorMsg(ValidationError errorMsg) {
      this.errorMsg = errorMsg;
    }

  }

  class Path {

    private final ArrayDeque segments;

    Path() {
      this.segments = new ArrayDeque();
    }

    protected Path(ArrayDeque segments) {
      this.segments = segments;
    }

    protected Path(Path orig, Object newSegment) {
      this.segments = orig.segments.clone();
      this.segments.push(newSegment);
    }

    Path push(Object indexOrKey) {
      return new Path(this, indexOrKey);
    }

    Path pop() {
      ArrayDeque segmentsCopy = segments.clone();
      segmentsCopy.pop();
      return new Path(segmentsCopy);
    }

    Object resolve(Object value) {
      if (length() == 0) {
        return value;
      }
      if (value == null) {
        return null;
      }
      if (value instanceof List) {
        Object index = segments.peek();
        if (!(index instanceof Integer)) {
          throw new RuntimeException("Invalid index type. Expected java.lang.Integer but got " + index.getClass().getName());
        }
        Object elem = ((List) value).get((Integer) index);
        return pop().resolve(elem);
      } else if (value instanceof Map) {
        Object key = segments.peek();
        Object elem = ((Map) value).get(key);
        return pop().resolve(elem);
      } else {
        throw new RuntimeException("Cannot resolve path over type " + value.getClass().getName());
      }
    }

    int length() {
      return segments.size();
    }
  }


}
