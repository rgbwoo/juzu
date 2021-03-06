/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.compiler;

import juzu.impl.common.MethodHandle;
import juzu.impl.common.Name;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class ElementHandle<E extends Element> implements Serializable {

  public static ElementHandle<?> create(Element elt) {
    ElementKind kind = elt.getKind();
    switch (kind) {
      case FIELD: {
        VariableElement variableElt = (VariableElement)elt;
        return Field.create(variableElt);
      }
      case CLASS: {
        TypeElement typeElt = (TypeElement)elt;
        return Type.create(typeElt);
      }
      case PACKAGE: {
        PackageElement packageElt = (PackageElement)elt;
        return Package.create(packageElt);
      }
      case METHOD: {
        ExecutableElement packageElt = (ExecutableElement)elt;
        return Method.create(packageElt);
      }
      default:
        throw new UnsupportedOperationException("Element " + elt + " with kind " + kind + " not supported");
    }
  }

  public final E get(ProcessingEnvironment env) {
    try {
      return doGet(env);
    }
    catch (RuntimeException e) {
      if (e.getClass().getName().equals("org.eclipse.jdt.internal.compiler.problem.AbortCompilation")) {
        // In case of eclipse we catch it and return null instead
        return null;
      }
      else {
        // Rethrow
        throw e;
      }
    }
  }

  protected abstract E doGet(ProcessingEnvironment env);

  public abstract Name getPackageName();

  public abstract boolean equals(Object obj);

  public abstract int hashCode();

  public abstract String toString();

  public static class Package extends ElementHandle<PackageElement> {

    public static Package create(Name packageName) {
      return new Package(packageName);
    }

    public static Package create(PackageElement elt) {
      return new Package(Name.parse(elt.getQualifiedName()));
    }

    /** . */
    private final Name name;

    private Package(Name name) {
      this.name = name;
    }

    @Override
    public Name getPackageName() {
      return name;
    }

    @Override
    protected PackageElement doGet(ProcessingEnvironment env) {
      return env.getElementUtils().getPackageElement(name);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof Package) {
        Package that = (Package)obj;
        return name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "ElementHandle.Package[name=" + name + "]";
    }
  }

  public static class Type extends ElementHandle<TypeElement> {

    public static Type create(Name fqn) {
      return new Type(fqn);
    }

    public static Type create(TypeElement elt) {
      return new Type(Name.parse(elt.getQualifiedName().toString()));
    }

    /** . */
    private final Name name;

    private Type(Name name) {
      this.name = name;
    }

    public Name getName() {
      return name;
    }

    @Override
    public Name getPackageName() {
      return name.getParent();
    }

    @Override
    protected TypeElement doGet(ProcessingEnvironment env) {
      return env.getElementUtils().getTypeElement(name);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof Type) {
        Type that = (Type)obj;
        return name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "ElementHandle.Class[name=" + name + "]";
    }
  }

  public static class Method extends ElementHandle<ExecutableElement> {

    public static Method create(String type, String name, String... parameterTypes) {
      ArrayList<String> tmp = new ArrayList<String>(parameterTypes.length);
      Collections.addAll(tmp, parameterTypes);
      return new Method(Name.parse(type), name, tmp);
    }

    public static Method create(java.lang.Class<?> type, String name, java.lang.Class<?>... parameterTypes) {
      String[] tmp = new String[parameterTypes.length];
      for (int i = 0;i < parameterTypes.length;i++) {
        tmp[i] = parameterTypes[i].getName();
      }
      return create(type.getName(), name, tmp);
    }

    public static Method create(Name fqn, String name, Collection<String> parameterTypes) {
      return new Method(fqn, name, new ArrayList<String>(parameterTypes));
    }

    public static Method create(ExecutableElement elt) {
      TypeElement typeElt = (TypeElement)elt.getEnclosingElement();
      String name = elt.getSimpleName().toString();
      Name fqn = Name.parse(typeElt.getQualifiedName().toString());
      ArrayList<String> parameterTypes = new ArrayList<String>();
      for (TypeMirror parameterType : ((ExecutableType)elt.asType()).getParameterTypes()) {
        parameterTypes.add(parameterType.toString());
      }

      return new Method(fqn, name, parameterTypes);
    }

    private Method(Name type, String name, ArrayList<String> parameterTypes) {
      this.type = new Type(type);
      this.name = name;
      this.parameterTypes = parameterTypes;
    }

    /** . */
    private final Type type;

    /** . */
    private final String name;

    /** . */
    private final ArrayList<String> parameterTypes;

    public Type getType() {
      return type;
    }

    public Name getTypeName() {
      return type.name;
    }

    @Override
    public Name getPackageName() {
      return type.name.getParent();
    }

    public String getName() {
      return name;
    }

    public List<String> getParameterTypes() {
      return parameterTypes;
    }

    @Override
    protected ExecutableElement doGet(ProcessingEnvironment env) {
      TypeElement typeElt = env.getElementUtils().getTypeElement(type.name);
      if (typeElt != null) {
        next:
        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
          if (executableElement.getSimpleName().toString().equals(name)) {
            List<? extends TypeMirror> parameterTypes = ((ExecutableType)executableElement.asType()).getParameterTypes();
            int len = parameterTypes.size();
            if (len == this.parameterTypes.size()) {
              for (int i = 0;i < len;i++) {
                if (!parameterTypes.get(i).toString().equals(this.parameterTypes.get(i))) {
                  continue next;
                }
              }
              return executableElement;
            }
          }
        }
      }
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof Method) {
        Method that = (Method)obj;
        return type.equals(that.type) && name.equals(that.name) && parameterTypes.equals(that.parameterTypes);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int hashCode = type.hashCode() ^ name.hashCode();
      for (String parameterType : parameterTypes) {
        hashCode = hashCode * 41 + parameterType.hashCode();
      }
      return hashCode;
    }

    public MethodHandle getMethodHandle() {
      return new MethodHandle(type.name.toString(), name, parameterTypes.toArray(new String[parameterTypes.size()]));
    }

    @Override
    public String toString() {
      return "ElementHandle.Method[type=" + type.name + ",name=" + name + ",parameterTypes" + parameterTypes + "]";
    }
  }

  public static class Field extends ElementHandle<VariableElement> {

    public static Field create(VariableElement elt) {
      TypeElement typeElt = (TypeElement)elt.getEnclosingElement();
      String name = elt.getSimpleName().toString();
      Name fqn = Name.parse(typeElt.getQualifiedName().toString());
      return new Field(fqn, name);
    }

    public static Field create(String fqn, String name) {
      return new Field(Name.parse(fqn), name);
    }

    public static Field create(Name fqn, String name) {
      return new Field(fqn, name);
    }

    /** . */
    private final Type type;

    /** . */
    private final String name;

    private Field(Name type, String name) {
      this.type = new Type(type);
      this.name = name;
    }

    public Type getType() {
      return type;
    }

    public Name getTypeName() {
      return type.name;
    }

    public String getName() {
      return name;
    }

    @Override
    public Name getPackageName() {
      return type.name.getParent();
    }

    @Override
    protected VariableElement doGet(ProcessingEnvironment env) {
      TypeElement typeElt = env.getElementUtils().getTypeElement(type.name);
      if (typeElt != null) {
        for (VariableElement variableElt : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
          if (variableElt.getSimpleName().contentEquals(name)) {
            return variableElt;
          }
        }
      }
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      else if (obj instanceof Field) {
        Field that = (Field)obj;
        return type.equals(that.type) && name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return type.hashCode() ^ name.hashCode();
    }

    @Override
    public String toString() {
      return "ElementHandle.Field[type=" + type.name + ",name=" + name + "]";
    }
  }
}
