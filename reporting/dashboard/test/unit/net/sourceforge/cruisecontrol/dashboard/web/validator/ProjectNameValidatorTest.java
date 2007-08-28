package net.sourceforge.cruisecontrol.dashboard.web.validator;

import junit.framework.TestCase;

public class ProjectNameValidatorTest extends TestCase {
    public void testShouldReturnInValidWhenProjectNameContainsInvalidCharacters() throws Exception {
        assertInvalidCharacter('\"');
        assertInvalidCharacter('!');
        assertInvalidCharacter(ProjectNameValidator.BRITISH_POUND);
        assertInvalidCharacter('$');
        assertInvalidCharacter('%');
        assertInvalidCharacter('^');
        assertInvalidCharacter('&');
        assertInvalidCharacter('*');
        assertInvalidCharacter('(');
        assertInvalidCharacter(')');
        assertInvalidCharacter('+');
        assertInvalidCharacter('=');
        assertInvalidCharacter('#');
        assertInvalidCharacter('~');
        assertInvalidCharacter('?');
        assertInvalidCharacter('/');
        assertInvalidCharacter('<');
        assertInvalidCharacter('>');
        assertInvalidCharacter('[');
        assertInvalidCharacter(']');
        assertInvalidCharacter('{');
        assertInvalidCharacter('}');
        assertInvalidCharacter('@');
        assertInvalidCharacter(':');
        assertInvalidCharacter(';');
        assertInvalidCharacter('\\');
        assertInvalidCharacter('\'');
        assertInvalidCharacter('|');
    }

    private void assertInvalidCharacter(char invalid) {
        ProjectNameValidator validator = new ProjectNameValidator("FooProject" + invalid);
        assertTrue(validator.isNotValid());
        assertEquals("'" + invalid + "' is not a valid character in a project name.", validator.error());
    }

    public void testShouldReturnValidWhenProjectNameIsOk() {
        ProjectNameValidator validator = new ProjectNameValidator("FooProject");
        assertFalse(validator.isNotValid());
    }

    public void testShouldReturnInValidWhenProjectNameIsBlank() {
        ProjectNameValidator validator = new ProjectNameValidator("");
        assertTrue(validator.isNotValid());
        assertEquals("Project name cannot be blank.", validator.error());

        validator = new ProjectNameValidator(" ");
        assertTrue(validator.isNotValid());
        assertEquals("Project name cannot be blank.", validator.error());

        validator = new ProjectNameValidator("\t");
        assertTrue(validator.isNotValid());
        assertEquals("Project name cannot be blank.", validator.error());

        validator = new ProjectNameValidator("\n");
        assertTrue(validator.isNotValid());
        assertEquals("Project name cannot be blank.", validator.error());
    }
}
