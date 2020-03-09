package test;

import com.gitlab.faerytea.mapper.annotations.Default;
import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;
import com.gitlab.faerytea.mapper.annotations.PutOnTypeArguments;
import com.gitlab.faerytea.mapper.converters.Convert;
import com.gitlab.faerytea.mapper.validation.NonNullValidator;
import com.gitlab.faerytea.mapper.validation.Validate;

import java.util.ArrayList;
import java.util.Objects;

import converter.CStyleEnumConverter;
import converter.CharSequenceCaster;
import test.generic.ContainsList;
import test.generic.GenericHolder;
import test.some.Test2;
import test.some.Test4;
import test.some.Test5;
import test.some.Test6;
import test.some.TestShape;
import unknown.BlowUp;

@Mappable(onUnknown = BlowUp.class)
public class Test {
    @Property
    public String first;
    @Property
    public String another;

    private String secret;

    @Property("named_property")
    public String namedProperty;

    @Validate(validator = NonNullValidator.class)
    @Property("final")
    public final String finalProperty;

    @Property
    public Test2 inner;

    @Property//(using = test.some.Test4Adapter.class)
    public Test4 dank;
    @Property//(using = test.some.Test5Adapter.class)
    public Test5 list;

    @Mappable
    public Test(String secret, @Property("final") String finalProperty) {
        this.secret = secret;
        this.finalProperty = finalProperty;
    }

    @Property
    public String getSecret() {
        return secret;
    }

    @Property
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Property
    @Validate("if ($v == null) throw new IllegalStateException(\"$t.$j is null!\");")
    public ContainsList containsList;

    @Property
    public GenericHolder<String> stringHolder;

    @Property
    public GenericHolder<ArrayList<GenericHolder<Test2>>> hell;

    @Property
    public GenericHolder<GenericHolder<GenericHolder<String>>> tripleGeneric;

    @PutOnTypeArguments({
            @PutOnTypeArguments.OnArg(false),
            @PutOnTypeArguments.OnArg(false),
            @PutOnTypeArguments.OnArg(convert = @Convert(CharSequenceCaster.class))})
    @Property
    public ArrayList<ArrayList<ArrayList<CharSequence>>> tripleList;

    @Default.Int(5)
    @Property
    public int defaultedToFive = 5;

    @Convert(CharSequenceCaster.class)
    @Property
    public CharSequence interfaced;

    @Convert(CharSequenceCaster.class)
    @Property
    public CharSequence interfaced2;

    @Property
    public ArrayList<Test6> blinking;

    @Convert(value = CStyleEnumConverter.class, named = "roshambo")
    @Property
    public int cEnumPaper;

    @Convert(value = CStyleEnumConverter.class, named = "weekdays")
    @Property
    public String day;

    private String name;

    public void setName(@Property("firstName") String fname, @Property("lastName") String name2) {
        name = fname + ':' + name2;
    }

    @Property("firstName")
    public String getFirstName() {
        return name == null ? null : name.split(":")[0];
    }

    @Property("lastName")
    public String getLastName() {
        return name == null ? null : name.split(":")[1];
    }

    @Property
    public ArrayList<TestShape> shapes;

    @Override
    public String toString() {
        return "Test{" +
                "first='" + first + '\'' +
                ", another='" + another + '\'' +
                ", secret='" + secret + '\'' +
                ", namedProperty='" + namedProperty + '\'' +
                ", finalProperty='" + finalProperty + '\'' +
                ", inner=" + inner +
                ", dank=" + dank +
                ", list=" + list +
                ", containsList=" + containsList +
                ", stringHolder=" + stringHolder +
                ", hell=" + hell +
                ", tripleGeneric=" + tripleGeneric +
                ", tripleList=" + tripleList +
                ", defaultedToFive=" + defaultedToFive +
                ", interfaced=" + interfaced +
                ", interfaced2=" + interfaced2 +
                ", blinking=" + blinking +
                ", cEnumPaper=" + cEnumPaper +
                ", day='" + day + '\'' +
                ", name='" + name + '\'' +
                ", shapes=" + shapes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test test = (Test) o;
        return defaultedToFive == test.defaultedToFive &&
                cEnumPaper == test.cEnumPaper &&
                Objects.equals(first, test.first) &&
                Objects.equals(another, test.another) &&
                Objects.equals(secret, test.secret) &&
                Objects.equals(namedProperty, test.namedProperty) &&
                Objects.equals(finalProperty, test.finalProperty) &&
                Objects.equals(inner, test.inner) &&
                Objects.equals(dank, test.dank) &&
                Objects.equals(list, test.list) &&
                Objects.equals(containsList, test.containsList) &&
                Objects.equals(stringHolder, test.stringHolder) &&
                Objects.equals(hell, test.hell) &&
                Objects.equals(tripleGeneric, test.tripleGeneric) &&
                Objects.equals(tripleList, test.tripleList) &&
                Objects.equals(interfaced, test.interfaced) &&
                Objects.equals(interfaced2, test.interfaced2) &&
                Objects.equals(blinking, test.blinking) &&
                Objects.equals(day, test.day) &&
                Objects.equals(name, test.name) &&
                Objects.equals(shapes, test.shapes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, another, secret, namedProperty, finalProperty, inner, dank, list, containsList, stringHolder, hell, tripleGeneric, tripleList, defaultedToFive, interfaced, interfaced2, blinking, cEnumPaper, day, name, shapes);
    }
}
