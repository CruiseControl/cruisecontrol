<?xml version="1.0"?>
<!-- Author: Andy Sadler <andy@slamjibe.com> -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html"/>
	<xsl:template match="/" mode="macker">
		<xsl:variable name="total.error.count" select="count(//macker-report//message)" />
		<xsl:if test="$total.error.count > 0">
			<table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
				<tr>
					<td class="macker-sectionheader" colspan="3"> Macker Errors,Warnings and Messages (<xsl:value-of select="$total.error.count" />) </td>
				</tr>
				<xsl:apply-templates select="cruisecontrol/macker-report//access-rule-violation/message" mode="macker">
					<xsl:sort select="../@severity" />
				</xsl:apply-templates>
			</table>
		</xsl:if>
	</xsl:template>
	<xsl:template match="message" mode="macker">
		<tr>
			<xsl:variable name="macker.data.style">
				<xsl:choose>
					<xsl:when test="../@severity='error'">
						<xsl:value-of select="'macker-data-error'"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="'macker-data'"/>
					</xsl:otherwise>
				</xsl:choose>
				</xsl:variable>
			<xsl:if test="position() mod 2 = 1">
				<xsl:attribute name="class">macker-oddrow</xsl:attribute>
			</xsl:if>						
			<td class="{$macker.data.style}">				
				<xsl:value-of select="../@severity" />
			</td>
			<td class="{$macker.data.style}">				
				<xsl:value-of select="." />
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="/">
		<xsl:apply-templates select="." mode="macker"/>
	</xsl:template>
</xsl:stylesheet>
