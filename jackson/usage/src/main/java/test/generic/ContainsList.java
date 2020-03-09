package test.generic;

import com.gitlab.faerytea.mapper.annotations.Default;
import com.gitlab.faerytea.mapper.annotations.Mappable;
import com.gitlab.faerytea.mapper.annotations.Property;

import java.util.ArrayList;
import java.util.Objects;

@Mappable
public class ContainsList {
    private ArrayList<String> list;

    @Property
    @Default("new ArrayList<>()")
    public ArrayList<String> getList() {
        return list;
    }

    @Property
    @Default("new ArrayList<>()")
    public void setList(ArrayList<String> list) {
        this.list = list;
    }

    @Property
    public int magic;

    @Override
    public String toString() {
        return "ContainsList{" +
                "list=" + list +
                ", magic=" + magic +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainsList that = (ContainsList) o;
        return magic == that.magic &&
                Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, magic);
    }
}
