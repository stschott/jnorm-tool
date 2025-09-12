package de.upb.sse.jnorm.core.sootup.typeresolving.types;

/*-
 * #%L
 * SootUp
 * %%
 * Copyright (C) 1997 - 2024 Raja Vallée-Rai and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Optional;
import org.jspecify.annotations.NonNull;
import sootup.core.jimple.visitor.TypeVisitor;
import sootup.core.types.*;

public class TopType extends Type {
  @NonNull private static final TopType INSTANCE = new TopType();

  @NonNull
  public static TopType getInstance() {
    return INSTANCE;
  }

  private TopType() {}

  @Override
  public <V extends TypeVisitor> V accept(@NonNull V typeVisitor) {
    /*typeVisitor.defaultCaseType();
    return typeVisitor;
    */
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "TopType";
  }

  @Override
  protected boolean isTopType() {
    return true;
  }

  @Override
  protected Type asTopType() {
    return this;
  }

  @Override
  protected Optional<Type> toTopType() {
    return Optional.of(this);
  }
}
