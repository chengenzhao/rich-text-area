package com.gluonhq.richtextarea;

import com.gluonhq.richtextarea.action.ActionFactory;
import com.gluonhq.richtextarea.model.Document;
import com.gluonhq.richtextarea.model.ParagraphDecoration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The RichTextArea control is a text input component that allows a user to enter multiple lines of
 * rich text and other non-text objects like images or hyperlinks.
 *
 * Internally the data model is based on a {@link com.gluonhq.richtextarea.model.PieceTable} implementation.
 * Pieces hold text and decorations that can be applied to style the text (like font or color) and
 * the paragraph with such text (like paragraph alignment). Unlimited undo/redo operations are allowed.
 *
 * A {@link com.gluonhq.richtextarea.model.DecorationModel} is used to represent text and paragraph decorations for
 * a given segment of content.
 *
 * A list of {@link com.gluonhq.richtextarea.model.DecorationModel} and the full text forms the {@link Document}, which
 * is the model that ultimately the control renders.
 *
 */
public class RichTextArea extends Control {

    public static final String STYLE_CLASS = "rich-text-area";
    private static final PseudoClass PSEUDO_CLASS_READONLY = PseudoClass.getPseudoClass("readonly");

    private final ActionFactory actionFactory = new ActionFactory(this);

    public RichTextArea() {
        getStyleClass().add(STYLE_CLASS);
    }

    // Properties

    // documentProperty
    /**
     * The {@link Document document} is the model that holds the full text and decorations that are being
     * displayed by the control.
     *
     * By default, this property is set via {@link ActionFactory#newDocument()} or {@link ActionFactory#open(Document)},
     * and gets updated only via {@link ActionFactory#save()}, unless {@link #autoSaveProperty()} is enabled, in which
     * the document gets updated after every change.
     *
     * @return the document for this control
     */
    public final ObjectProperty<Document> documentProperty() {
       return documentProperty;
    }
    public final Document getDocument() {
       return documentProperty.get();
    }
    public final void setDocument(Document value) {
        documentProperty.set(value);
    }
    private final ObjectProperty<Document> documentProperty = new SimpleObjectProperty<>(this, "document", new Document());

    // autoSaveProperty
    /**
     * Property that allows saving every change done into the {@link Document document}.
     * By default, it is disabled, and it is recommended to use the {@link ActionFactory#save()} action
     * instead, on user's demand: If auto save is enabled, there might be some impact on performance.
     *
     * @return if auto saving is enabled or not
     */
    public final BooleanProperty autoSaveProperty() {
       return autoSaveProperty;
    }
    public final boolean isAutoSave() {
       return autoSaveProperty.get();
    }
    public final void setAutoSave(boolean value) {
        autoSaveProperty.set(value);
    }
    private final BooleanProperty autoSaveProperty = new SimpleBooleanProperty(this, "autoSave");

    // modifiedProperty
    /**
     * Indicates if the current {@link Document document} has unsaved changes or not.
     *
     * Unless {@link #autoSaveProperty()} is enabled, after any change of the document being edited with the control
     * this property will be set to true, and will enable the {@link ActionFactory#save()} action.
     *
     * @return if the document is modified or not
     */
    public final ReadOnlyBooleanProperty modifiedProperty() {
       return modifiedProperty.getReadOnlyProperty();
    }
    public final boolean isModified() {
       return modifiedProperty.get();
    }
    final ReadOnlyBooleanWrapper modifiedProperty = new ReadOnlyBooleanWrapper(this, "modified");

    // editableProperty
    /**
     * Indicates if the {@link Document document} is editable or not.
     *
     * By default, it is set to true.
     *
     * @return if the document is editable or not
     */
    public final BooleanProperty editableProperty() {
       return editableProperty;
    }
    public final boolean isEditable() {
       return editableProperty.get();
    }
    public final void setEditable(boolean value) {
        editableProperty.set(value);
    }
    private final BooleanProperty editableProperty = new SimpleBooleanProperty(this, "editable", true) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(PSEUDO_CLASS_READONLY, !get());
        }
    };

    // selectionProperty
    /**
     *  Contains the {@link Selection selection} of some fragment of the document, if any.
     *
     * @return the existing selection, if any
     */
    public final ReadOnlyObjectProperty<Selection> selectionProperty() {
       return selectionProperty.getReadOnlyProperty();
    }
    public final Selection getSelection() {
       return selectionProperty.get();
    }
    private final ReadOnlyObjectWrapper<Selection> selectionProperty = new ReadOnlyObjectWrapper<>(this, "selection", Selection.UNDEFINED);

    // textLengthProperty
    /**
     * Returns the current length of the {@link Document document}, and it gets updated after every change,
     * even if the document has not been saved yet.
     *
     * @return the current length of the document
     */
    public final ReadOnlyIntegerProperty textLengthProperty() {
        return textLengthProperty.getReadOnlyProperty();
    }
    public final int getTextLength() {
        return textLengthProperty.get();
    }
    final ReadOnlyIntegerWrapper textLengthProperty = new ReadOnlyIntegerWrapper(this, "textLength");

    // contentAreaWidthProperty
    /**
     * Defines a width constraint for the content area of the rich text control,
     * in user space coordinates, where text can be added.
     * The width is measured in pixels (and not glyph or character count).
     * If the value is {@code <= 0}, the content area extends to the whole viewport of control
     * and will change whenever the viewport gets resized.
     * If the value is {@code > 0}, the content area is exactly set to this value,
     * and the control will provide a horizontal scrollbar if needed.
     *
     * In any case, text will be line wrapped as needed to satisfy this constraint.
     *
     * @defaultValue 0
     *
     * @return the width of the content area
     */
    public final DoubleProperty contentAreaWidthProperty() {
        return contentAreaWidthProperty;
    }
    public final double getContentAreaWidth() {
        return contentAreaWidthProperty.get();
    }
    public final void setContentAreaWidth(double value) {
        contentAreaWidthProperty.set(value);
    }
    private final DoubleProperty contentAreaWidthProperty = new SimpleDoubleProperty(this, "contentAreaWidth", 0d);

    // paragraphGraphicFactoryProperty
    /**
     * A paragraph can be decorated with a node to the left. This property allows adding the graphic for
     * numbered or bulleted lists.
     *
     * Once the {@link ParagraphDecoration#getIndentationLevel() indentation level} and the
     * {@link ParagraphDecoration.GraphicType graphic type} for a given paragraph are set,
     * for instance with the {@link com.gluonhq.richtextarea.action.ParagraphDecorateAction},
     * the {@link BiFunction} allows defining the node that will be used for the graphic with
     * such indentation level and type of list.
     *
     * Numbered lists should use a {@link javafx.scene.control.Label} as node. The text of the label should contain,
     * at least, an {@code "#"} as a wildcard for the index of paragraph, which will be automatically determined
     * by the control.
     *
     * By default, the control provides a {@link DefaultParagraphGraphicFactory factory}.
     *
     * @return a factory to define the graphic decoration for each paragraph, if any
     */
    public final ObjectProperty<BiFunction<Integer, ParagraphDecoration.GraphicType, Node>> paragraphGraphicFactoryProperty() {
       return paragraphGraphicFactoryProperty;
    }
    public final BiFunction<Integer, ParagraphDecoration.GraphicType, Node> getParagraphGraphicFactory() {
       return paragraphGraphicFactoryProperty.get();
    }
    public final void setParagraphGraphicFactory(BiFunction<Integer, ParagraphDecoration.GraphicType, Node> value) {
        paragraphGraphicFactoryProperty.set(value);
    }
    private final ObjectProperty<BiFunction<Integer, ParagraphDecoration.GraphicType, Node>> paragraphGraphicFactoryProperty =
            new SimpleObjectProperty<>(this, "paragraphGraphicFactory", DefaultParagraphGraphicFactory.getFactory());

    // linkCallbackFactoryProperty
    /**
     * Allows setting a consumer that accepts a valid URL string, for a given node.
     *
     * Typically, this can be applied to {@link javafx.scene.text.Text} or {@link javafx.scene.image.ImageView} nodes
     * with a link to a given URL.
     *
     * A default {@link DefaultLinkCallbackFactory factory} allows opening this link in
     * the browser.
     *
     * @return a factory to process links to URL strings
     */
    public final ObjectProperty<Function<Node, Consumer<String>>> linkCallbackFactoryProperty() {
       return linkCallbackFactoryProperty;
    }
    public final Function<Node, Consumer<String>> getLinkCallbackFactory() {
       return linkCallbackFactoryProperty.get();
    }
    public final void setLinkCallbackFactory(Function<Node, Consumer<String>> value) {
        linkCallbackFactoryProperty.set(value);
    }
    private final ObjectProperty<Function<Node, Consumer<String>>> linkCallbackFactoryProperty =
            new SimpleObjectProperty<>(this, "linkCallbackFactory", DefaultLinkCallbackFactory.getFactory());

    // public methods

    /**
     * The action factory that can be used from toolBars, menus or context menus to
     * apply given actions, like {@link ActionFactory#save()}, {@link ActionFactory#copy()}
     * or {@link ActionFactory#undo()}, to the {@link Document document}.
     *
     * @return the action factory
     */
    public final ActionFactory getActionFactory() {
        return actionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SkinBase<RichTextArea> createDefaultSkin() {
        return new RichTextAreaSkin(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String getUserAgentStylesheet() {
        return getClass().getResource("rich-text-area.css").toExternalForm();
    }

}

