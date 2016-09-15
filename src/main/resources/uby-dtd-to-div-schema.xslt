<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
        >
    <xsl:template match="@*|node()|comment()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()|comment()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="xs:element/@ref"/>
    <xsl:template match="xs:element[@ref]">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:attribute name="type"><xsl:value-of select="@ref"/></xsl:attribute>
            <xsl:attribute name="name"><xsl:value-of select="@ref"/></xsl:attribute>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="xs:element[@name = //xs:element/@ref and xs:complexType]">
        <xs:complexType name="{@name}">
            <xsl:apply-templates select="xs:complexType/node()"/>
        </xs:complexType>
    </xsl:template>
    <xsl:template match="xs:element[@name = //xs:element/@ref and @type]">
        <xsl:choose>
            <xsl:when test="//xs:complexType[@name = current()/@type]">
                <xs:complexType name="{@name}">
                    <xs:complexContent>
                        <xs:extension base="{@type}"/>
                    </xs:complexContent>
                </xs:complexType>
            </xsl:when>
            <xsl:otherwise>
                <xs:simpleType name="{@name}">
                    <xs:restriction base="{@type}"/>
                </xs:simpleType>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>