package cz.vity.freerapid.plugins.webclient.utils;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import junit.framework.Assert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

/**
 * @author tong2shot
 *         <p>
 *         Reference: https://jsoup.org/cookbook/extracting-data/
 */
public class JsoupTest {

    @Test
    public void testJsoup() throws PluginImplementationException {
        String content = "\n" +
                "<table class=\"display\" style=\"margin-bottom: 20px;\">\n" +
                "    <tr class=\"rowheader\">\n" +
                "        <th style=\"width: 220px;\">Console</th>\n" +
                "        <th style=\"width: 220px;\">Genre</th>\n" +
                "        <th>Developers</th>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow0\">\n" +
                "        <td style=\"vertical-align: top;\">\n" +
                "            <a href=\"/playstation_3/\">Playstation 3</a>\n" +
                "        </td>\n" +
                "        <td style=\"vertical-align: top;\">\n" +
                "            <a href=\"/genre/fighting/\">Fighting</a>\n" +
                "        </td>\n" +
                "        <td style=\"vertical-align: top;\">Developer coming soon!</td>\n" +
                "    </tr>\n" +
                "    <tr class=\"rowheader\">\n" +
                "        <th colspan=\"3\" class=\"noborder\">Tags</th>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow0\">\n" +
                "        <td colspan=\"3\" class=\"noborder\">\n" +
                "            Tagging is in the works and will be here soon!\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "</table>\n" +
                "\n" +
                "<table class=\"display\" style=\"width: 500px; height: 240px;\">\n" +
                "    <tr class=\"rowheader\">\n" +
                "        <th colspan=\"2\" class=\"noborder\">\n" +
                "            <div style=\"width: 380px; float: left; clear: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;\"\n" +
                "                                     title=\"Akira Yuki\">Akira Yuki\n" +
                "            </div>\n" +
                "        </th>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow0\">\n" +
                "        <td style=\"width: 100px;\">Game</td>\n" +
                "        <td>\n" +
                "            <div style=\"width: 400px; float: left; clear: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;\">\n" +
                "                <a href=\"/playstation_3/dengekibunkofightingclimaxignition/\"\n" +
                "                                       title=\"Dengeki Bunko: Fighting Climax Ignition\">Dengeki Bunko: Fighting Climax\n" +
                "                    Ignition\n" +
                "                </a>\n" +
                "            </div>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow1\">\n" +
                "        <td>Section</td>\n" +
                "        <td>\n" +
                "            <div style=\"width: 380px; float: left; clear: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;\"\n" +
                "                                     title=\"Battle Sprites (Playable Characters)\">Battle Sprites (Playable Characters)\n" +
                "            </div>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow0\">\n" +
                "        <td>Filesize</td>\n" +
                "        <td>8.43 MB</td>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow1\">\n" +
                "        <td>Submitters</td>\n" +
                "        <td>\n" +
                "            <a href=\"/submitter/puggsoy/\">puggsoy</a>, <a href=\"/submitter/Sosfiro/\">Sosfiro</a>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow0\">\n" +
                "        <td>Format</td>\n" +
                "        <td>ZIP</td>\n" +
                "    </tr>\n" +
                "    <tr class=\"altrow1\">\n" +
                "        <td>Hits</td>\n" +
                "        <td>816</td>\n" +
                "    </tr>\n" +
                "    <tr class=\"rowfooter\">\n" +
                "        <th colspan=\"2\" class=\"noborder\">\n" +
                "            <div style=\"float: left;\">\n" +
                "                <a href=\"/download/86023/\">Download this Sheet</a>\n" +
                "            </div>\n" +
                "            <div style=\"float: right;\">\n" +
                "                <a href=\"/playstation_3/dengekibunkofightingclimaxignition/\">Return\n" +
                "                    to Game\n" +
                "                </a>\n" +
                "            </div>\n" +
                "        </th>\n" +
                "    </tr>\n" +
                "</table>\n";

        Document document = Jsoup.parse(content);
        document.setBaseUri("http://example.com/");
        Element element = document.select(".altrow0:contains(Game) a").first();
        String gameName = PlugUtils.unescapeHtml(element.text());
        element = document.select(".altrow1:contains(Section) div").first();
        String sectionName = PlugUtils.unescapeHtml(element.text());

        Element genreElement = document.select(".display:contains(Genre) .altrow0 td:eq(1) a[href]").first(); //:eq(n) = sibling index from parent is equal to n
        String genre = PlugUtils.unescapeHtml(genreElement.text().trim());
        genreElement = document.select("a[href*=/genre/]").first(); //attr contains
        String genre2 = PlugUtils.unescapeHtml(genreElement.text().trim());
        String genreUrl = genreElement.attr("href"); //relative URL in this case
        String genreAbsUrl = genreElement.absUrl("href"); //absolute URL

        Element gameElement = document.select("a[href]:contains(Return)").first();
        String gameAbsUrl = gameElement.absUrl("href");

        Element filesizeElement = document.select("td:matches(Filesize) + td").first(); //next sibling
        long filesize = PlugUtils.getFileSizeFromString(filesizeElement.text());

        Assert.assertEquals(gameName, "Dengeki Bunko: Fighting Climax Ignition");
        Assert.assertEquals(sectionName, "Battle Sprites (Playable Characters)");
        Assert.assertEquals(genre, "Fighting");
        Assert.assertEquals(genre, genre2);
        Assert.assertEquals(genreUrl, "/genre/fighting/");
        Assert.assertEquals(genreAbsUrl, "http://example.com/genre/fighting/");
        Assert.assertEquals(gameAbsUrl, "http://example.com/playstation_3/dengekibunkofightingclimaxignition/");
        Assert.assertEquals(filesize, PlugUtils.getFileSizeFromString("8.43 MB"));
    }

}
