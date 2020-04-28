package command.util.misc;

public class CourseData {

    private String courseId;
    private String restrictions;
    private String prerequisites;
    private String description;

    public CourseData() {
        this("", "", "", "");
    }

    public CourseData(String courseId, String restrictions, String prerequisites, String description) {
        this.courseId = courseId;
        this.restrictions = restrictions;
        this.prerequisites = prerequisites;
        this.description = description;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
