package org.esa.snap.ui.product.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.openide.nodes.AbstractNode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.esa.snap.core.datamodel.ProductData.*;

/**
 * @author Tonio Fincke
 */
public class MetadataTableInnerElement implements MetadataTableElement {

    private final MetadataElement metadataElement;
    private final MetadataTableElement[] metadataTableElements;


    public MetadataTableInnerElement(MetadataElement metadataElement) {
        this.metadataElement = metadataElement;
        metadataTableElements = getChildrenElementsFromElement(metadataElement);
    }

    @Override
    public MetadataTableElement[] getMetadataTableElements() {
        return metadataTableElements;
    }

    @Override
    public String getName() {
        return metadataElement.getName();
    }

    @Override
    public AbstractNode createNode() {
        return new MetadataElementInnerNode(this);
    }

    private static MetadataTableElement[] getChildrenElementsFromElement(MetadataElement metadataElement) {
        MetadataElement[] elements = metadataElement.getElements();
        MetadataAttribute[] attributes = metadataElement.getAttributes();
        List<MetadataTableElement> metadataTableElementList = new ArrayList<>();
        for (MetadataElement element : elements) {
            metadataTableElementList.add(new MetadataTableInnerElement(element));
        }
        for (MetadataAttribute attribute : attributes) {
            final long dataElemSize = attribute.getNumDataElems();
            if (dataElemSize > 1) {
                final int dataType = attribute.getDataType();
                ProductData data = attribute.getData();
                if ((ProductData.isFloatingPointType(dataType) || ProductData.isIntType(dataType)) && !(data instanceof ProductData.UTC)) {
                    addMetadataAttributes(attribute, data, metadataTableElementList);
                } else {
                    metadataTableElementList.add(new MetadataTableLeaf(attribute));
                }
            } else {
                metadataTableElementList.add(new MetadataTableLeaf(attribute));
            }
        }
        return metadataTableElementList.toArray(new MetadataTableElement[metadataTableElementList.size()]);
    }

    private static void addMetadataAttributes(MetadataAttribute attribute, ProductData data,
                                                   List<MetadataTableElement> metadataTableElementList) {
        final String name = attribute.getName();
        final int dataType = attribute.getDataType();
        final String unit = attribute.getUnit();
        final String description = attribute.getDescription();
        for (int i = 0; i < data.getNumElems(); i++) {
            final MetadataAttribute partAttribute = new MetadataAttribute(name + "." + (i + 1), dataType);
            try {
                partAttribute.setDataElems(getDataElemArray(data, i));
            } catch (IllegalArgumentException e) {
                String msg = String.format("Not able to set metadata value for '%s': %s", name, e.getMessage());
                SystemUtils.LOG.log(Level.SEVERE, msg, e);
            }
            partAttribute.setUnit(unit);
            partAttribute.setDescription(description);
            metadataTableElementList.add(new MetadataTableLeaf(partAttribute));
        }
    }

    private static Object getDataElemArray(ProductData data, int index) {
        switch (data.getType()) {
            case TYPE_INT8:
                return new byte[]{(byte)data.getElemIntAt(index)};
            case TYPE_INT16:
                return new short[]{(short)data.getElemIntAt(index)};
            case TYPE_INT32:
                return new int[]{data.getElemIntAt(index)};
            case TYPE_UINT8:
                return new byte[]{(byte)data.getElemUIntAt(index)};
            case TYPE_UINT16:
                return new short[]{(short)data.getElemUIntAt(index)};
            case TYPE_UINT32:
                return new int[]{(int)data.getElemUIntAt(index)};
            case TYPE_INT64:
                return new long[]{data.getElemLongAt(index)};
            case TYPE_FLOAT32:
                return new float[]{data.getElemFloatAt(index)};
            case TYPE_FLOAT64:
                return new double[]{data.getElemDoubleAt(index)};
            default:
                return null;
        }
    }
}
