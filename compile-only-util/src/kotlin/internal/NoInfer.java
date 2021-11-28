package kotlin.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SuppressWarnings("unused") // IDE false positive warning. It's actually used!
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.CLASS)
public @interface NoInfer {
}
