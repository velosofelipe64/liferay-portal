/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.admin.taxonomy.client.dto.v1_0.TaxonomyCategory;
import com.liferay.headless.admin.taxonomy.client.dto.v1_0.TaxonomyVocabulary;
import com.liferay.headless.admin.taxonomy.client.pagination.Page;
import com.liferay.headless.admin.taxonomy.client.pagination.Pagination;
import com.liferay.headless.admin.taxonomy.client.resource.v1_0.TaxonomyCategoryResource;
import com.liferay.headless.admin.taxonomy.client.resource.v1_0.TaxonomyVocabularyResource;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;
import com.liferay.headless.delivery.client.dto.v1_0.Document;
import com.liferay.headless.delivery.client.resource.v1_0.DocumentResource;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;

import java.net.URL;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Raymond Augé
 * @author Gregory Amerson
 * @author Brian Wing Shun Chan
 */
@RequestMapping("/ready")
@RestController
public class ReadyRestController extends BaseRestController {

	@GetMapping
	public String get() throws Exception {
		try {
			System.out.println("Started");

			_processAuthors();

			_loadTaxonomyCategories();

			_migrateZendeskArticles("en-us");
			_migrateZendeskArticles("ja");
		}
		finally {
			System.out.println("FINISHED");
		}

		return "READY";
	}

	private boolean _remove() {
		for (int page = 1;; page++) {
			JSONObject jsonObject = new JSONObject(get(_getLiferayAuthorization(), _getLiferayURL() + "/o/c/p2s3knowledgearticles/scopes/guest?page="+ page+"&pageSize=500"));

			JSONArray items = jsonObject.getJSONArray("items");

			for (int i = 0; i < items.length(); i++) {
				JSONObject jsonObject1 = items.getJSONObject(i);
				delete(_getLiferayAuthorization(), "", _getLiferayURL() + "/o/c/p2s3knowledgearticles/"+ jsonObject1.getLong("id"));
			}

			if (page == jsonObject.getInt("lastPage")){
				break;
			}

		}


		return true;
	}

	@Override
	protected String getWebClientBaseURL() {
		return "";
	}

	private void _addLocalization(
		JSONObject jsonObject, String localizationLabel) {

		if (_zendeskLocaleMap.get(localizationLabel) == null) {
			return;
		}

		JSONObject localizedArticle = null;

		try {
			localizedArticle = new JSONObject(
				get(
					_getZendeskAuthorization(),
					_zendeskUrl + "/api/v2/help_center/" +
						localizationLabel + "/articles/" +
							jsonObject.getLong("externalReferenceCode"))
			).getJSONObject(
				"article"
			);
		}
		catch (Exception exception) {
			System.out.println(
				"Unable to get locale " + localizationLabel + " for " +
					jsonObject.toString());

			return;
		}

		JSONObject contentI18nJSONObject = jsonObject.getJSONObject(
			"content_i18n");

		contentI18nJSONObject.put(
			_zendeskLocaleMap.get(localizationLabel),
			localizedArticle.getString("body"));

		JSONObject titleI18nJSONObject = jsonObject.getJSONObject("title_i18n");

		titleI18nJSONObject.put(
			_zendeskLocaleMap.get(localizationLabel),
			localizedArticle.getString("title"));
	}

	private long _getAuthorId(long authorId) throws Exception {
		JSONObject jsonObject = _authorIdMap.get(authorId);

		long userId = 0;

		try {
			if (jsonObject.has("userId")) {
				userId = jsonObject.getLong("userId");
			}
		}
		catch (Exception exception) {
			userId = -1;
			System.out.println("INFERNO += " + authorId);
		}

		if (userId != 0) {
			return userId;
		}

		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			new URL(_getLiferayURL())
		).header(
			HttpHeaders.AUTHORIZATION, _getLiferayAuthorization()
		).build();

		UserAccount userAccount = null;

		try {
			userAccount = userAccountResource.getUserAccountByEmailAddress(
				jsonObject.getString("email"));
		}
		catch (Exception exception) {
			userAccount = new UserAccount();

			userAccount.setName(() -> jsonObject.getString("name"));

			userAccount.setEmailAddress(() -> jsonObject.getString("email"));
			userAccount.setExternalReferenceCode(
				() -> String.valueOf(jsonObject.getLong("id")));
			userAccount.setAlternateName(
				() -> StringUtil.extractFirst(
					jsonObject.getString("email"), "@"));
			userAccount.setGivenName(
				() -> StringUtil.extractFirst(
					jsonObject.getString("name"), " "));
			userAccount.setFamilyName(
				() -> StringUtil.extractLast(
					jsonObject.getString("name"), " "));

			try {
				userAccount = userAccountResource.postUserAccount(userAccount);
			}
			catch (Exception exception1) {
				System.out.println(
					"Unable to create user " + userAccount.toString());

				return 0;
			}
		}

		jsonObject.put("userId", userAccount.getId());

		return userAccount.getId();
	}

	private JSONObject _getKnowledgeArticleJSONObject(
			JSONObject jsonObject, String locale)
		throws Exception {

		if (locale.equals("ja")) {
			try {
				get(
					_getLiferayAuthorization(),
					_getLiferayURL() + "/o/c/p2s3knowledgearticles/"+
						"by-external-reference-code/" +
							jsonObject.getLong("id"));

				return null;
			}
			catch (Exception exception) {
				System.out.println("Adding new JA article");
			}
		}

		JSONObject knowledgeArticleJSONObject = new JSONObject();

		JSONObject authorJSONObject = _authorIdMap.get(jsonObject.getLong("author_id"));

		if(authorJSONObject == null){
			authorJSONObject = new JSONObject(
					get(
							_getZendeskAuthorization(),
							_zendeskUrl + "/api/v2/users/" +
									jsonObject.getLong("author_id"))).getJSONObject("user");

			_authorIdMap.put(jsonObject.getLong("author_id"), authorJSONObject);
		}

		knowledgeArticleJSONObject.put(
			"authorName", authorJSONObject.getString("name")
		).put(
			"authorEmailAddress", authorJSONObject.getString("email")
		).put(
			"content_i18n",
			new JSONObject(
			).put(
				_zendeskLocaleMap.get(locale), jsonObject.getString("body")
			)
		).put(
			"externalReferenceCode", jsonObject.getLong("id")
		).put(
			"legacy", true
		).put(
			"name", jsonObject.getString("name")
		).put(
			"showDisclaimerMessage", true
		).put(
			"sourceTeam", "Support"
		).put(
			"title_i18n",
			new JSONObject(
			).put(
				_zendeskLocaleMap.get(locale), jsonObject.getString("title")
			)
		).put(
			"taxonomyCategoryIds",
			_processLabels(
				jsonObject.getJSONArray("label_names"),
				knowledgeArticleJSONObject
			).toArray(
				new Long[0]
			)
		);

		try {
			knowledgeArticleJSONObject = new JSONObject(
				put(
					_getLiferayAuthorization(),
					knowledgeArticleJSONObject.toString(),
					_getLiferayURL() + "/o/c/p2s3knowledgearticles/" +
						"by-external-reference-code/" +
							knowledgeArticleJSONObject.getLong(
								"externalReferenceCode")));
		}
		catch (Exception exception) {
			System.out.println(
				"Unable to add/update " +
					knowledgeArticleJSONObject.toString());

			return null;
		}

		try {
			JSONArray articleAttachmentsJSONArray = new JSONObject(
				get(
					_getZendeskAuthorization(),
					_zendeskUrl + "/api/v2/help_center/articles/" +
						jsonObject.getLong("id") + "/attachments")
			).getJSONArray(
				"article_attachments"
			);

			if (articleAttachmentsJSONArray.isEmpty()) {
				return knowledgeArticleJSONObject;
			}

			for (int i = 0; i < articleAttachmentsJSONArray.length(); i++) {
				JSONObject articleAttachmentJSONObject =
					articleAttachmentsJSONArray.getJSONObject(i);

				DocumentResource.Builder documentResourceBuilder =
					DocumentResource.builder();

				DocumentResource documentResource =
					documentResourceBuilder.header(
						"Authorization", _getLiferayAuthorization()
					).endpoint(
						new URL(_getLiferayURL())
					).build();

				Document document = null;

				try {
					document =
						documentResource.getSiteDocumentByExternalReferenceCode(
							_siteGroupId,
							String.valueOf(
								articleAttachmentJSONObject.getLong("id")));
				}
				catch (Exception exception) {
					WebClient webClient = WebClient.builder(
					).exchangeStrategies(
						ExchangeStrategies.builder(
						).codecs(
							codecs -> codecs.defaultCodecs(
							).maxInMemorySize(
								(int)DataSize.ofMegabytes(
									1000
								).toBytes()
							)
						).build()
					).build();

					ResponseEntity<byte[]> contentUrl = null;

					try {
						contentUrl = webClient.get(
						).uri(
							articleAttachmentJSONObject.getString("content_url")
						).retrieve(
						).toEntity(
							byte[].class
						).block();
					}
					catch (Exception exception1) {
						continue;
					}

					File tempFile = File.createTempFile("tmp", ".txt");

					tempFile.deleteOnExit();

					try (FileOutputStream fileOutputStream =
							new FileOutputStream(tempFile)) {

						fileOutputStream.write(contentUrl.getBody());
					}

					document = new Document();

					document.setViewableBy(Document.ViewableBy.ANYONE);
					document.setExternalReferenceCode(
						String.valueOf(
							articleAttachmentJSONObject.getLong("id")));
					document.setFriendlyUrlPath(
						String.valueOf(
							articleAttachmentJSONObject.getLong("id")));
					document.setFileName(
						String.valueOf(
							articleAttachmentJSONObject.getLong("id")));

					documentResource.postDocumentFolderDocument(
						_documentFolderId, document,
						Collections.singletonMap("file", tempFile));

					String fileName = articleAttachmentJSONObject.getString(
						"file_name");

					String extension = ".";

					if (fileName.lastIndexOf('.') != -1) {
						extension = fileName.substring(
							fileName.lastIndexOf('.'));
					}
					else {
						continue;
					}

					document.setFileName(
						articleAttachmentJSONObject.getLong("id") + extension);

					document =
						documentResource.putSiteDocumentByExternalReferenceCode(
							_siteGroupId, document.getExternalReferenceCode(),
							document,
							Collections.singletonMap("file", tempFile));
				}

				try {
					JSONObject attachmentJSONObject = new JSONObject(
							put(
							_getLiferayAuthorization(),
							new JSONObject(
							).put(
									"name",
									articleAttachmentJSONObject.getString("file_name")
							).put(
									"file", document.getId()
							).put(
									"r_p2s3KnowledgeArticleToP2S3Attachments_c_p2s3KnowledgeArticleERC",
									String.valueOf(jsonObject.getLong("id"))
							).put(
									"externalReferenceCode",
									String.valueOf(
											articleAttachmentJSONObject.getLong("id"))
							).toString(),
							_getLiferayURL() +
									"/o/c/p2s3knowledgearticleattachments/by-external-reference-code/" +
									document.getExternalReferenceCode()));

					try {
						put(
								_getLiferayAuthorization(),
								new JSONArray(
								).put(
										new JSONObject(
										).put(
												"actionIds", new String[] {"VIEW"}
										).put(
												"roleName", "guest"
										)
								).toString(),
								_getLiferayURL() + "/o/c/p2s3knowledgearticleattachments/" +
										attachmentJSONObject.getLong("id") + "/permissions");
					}
					catch (Exception exception) {
						System.out.println(
								"Unable to set permissions for object entry Id " +
										attachmentJSONObject.getLong("id"));
					}
				}
				catch (Exception exception) {
					System.out.println(
						"Unable to add or update attachment for ");
					System.out.println(
						new JSONObject(
						).put(
							"name",
							articleAttachmentJSONObject.getString("file_name")
						).put(
							"file", document.getId()
						).put(
							"r_p2s3KnowledgeArticleToP2S3Attachments_c_p2s3KnowledgeArticleERC",
							String.valueOf(jsonObject.getLong("id"))
						).put(
							"externalReferenceCode",
							String.valueOf(
								articleAttachmentJSONObject.getLong("id"))
						).toString());
				}
			}
		}
		catch (Exception exception) {
			System.out.println(
				"SOMETHING HAPPENED ON ARTICLE ID " + jsonObject.getLong("id"));

			System.out.println(exception.getMessage());
		}

		return knowledgeArticleJSONObject;
	}

	private String _getLiferayAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private String _getLiferayURL() {
		return lxcDXPServerProtocol + "://" + lxcDXPMainDomain;
	}

	private String _getZendeskAuthorization() {
		return "Bearer " + _liferayZendeskApiToken;
	}

	private void _loadTaxonomyCategories() throws Exception {
		TaxonomyVocabularyResource.Builder taxonomyVocabularyResourceBuilder =
			TaxonomyVocabularyResource.builder();

		TaxonomyVocabularyResource taxonomyVocabularyResource =
			taxonomyVocabularyResourceBuilder.header(
				"Authorization", _getLiferayAuthorization()
			).endpoint(
				new URL(_getLiferayURL())
			).build();

		TaxonomyCategoryResource.Builder taxonomyCategoryResourceBuilder =
			TaxonomyCategoryResource.builder();

		TaxonomyCategoryResource taxonomyCategoryResource =
			taxonomyCategoryResourceBuilder.header(
				"Authorization", _getLiferayAuthorization()
			).endpoint(
				new URL(_getLiferayURL())
			).build();

		Page<TaxonomyVocabulary> taxonomyVocabulariesPage =
			taxonomyVocabularyResource.getSiteTaxonomyVocabulariesPage(
				_siteGroupId, null, null, null, Pagination.of(-1, -1), null);

		for (TaxonomyVocabulary taxonomyVocabulary :
				taxonomyVocabulariesPage.getItems()) {

			Page<TaxonomyCategory> taxonomyCategoriesPage =
				taxonomyCategoryResource.
					getTaxonomyVocabularyTaxonomyCategoriesPage(
						taxonomyVocabulary.getId(), true, null, null, null,
						Pagination.of(-1, -1), null);

			for (TaxonomyCategory taxonomyCategory :
					taxonomyCategoriesPage.getItems()) {

				_taxonomyCategories.put(
					taxonomyCategory.getName(), taxonomyCategory.getId());
			}
		}
	}

	private void _migrateZendeskArticles(String locale) throws Exception {
		for (int page =1;; page++) {
			JSONObject jsonObject = new JSONObject(
				get(
					_getZendeskAuthorization(),
					_zendeskUrl + "/api/v2/help_center/" +
						locale + "/articles?label_names=MIGRATION 1&page=" +
							page + "&per_page=100"));

			JSONArray jsonArray = jsonObject.getJSONArray("articles");

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject1 = _getKnowledgeArticleJSONObject(
					jsonArray.getJSONObject(i), locale);

				if (jsonObject1 == null) {
					continue;
				}

				try {
					put(
						_getLiferayAuthorization(),
						new JSONArray(
						).put(
							new JSONObject(
							).put(
								"actionIds", new String[] {"VIEW"}
							).put(
								"roleName", "guest"
							)
						).toString(),
						_getLiferayURL() + "/o/c/p2s3knowledgearticles/" +
							jsonObject1.getLong("id") + "/permissions");
				}
				catch (Exception exception) {
					System.out.println(
						"Unable to set permissions for object entry Id " +
							jsonObject1.getLong("id"));
				}
			}

			if (jsonObject.getInt("page_count") == page) {
				break;
			}
		}
	}

	private void _processAuthors() {
		for (Long authorId : _AUTHOR_IDS) {
			JSONObject jsonObject = new JSONObject(
				get(
					_getZendeskAuthorization(),
					_zendeskUrl + "/api/v2/users/" +
						authorId));

			_authorIdMap.put(authorId, jsonObject.getJSONObject("user"));
		}
	}

	private Collection<Long> _processLabels(
		JSONArray jsonArray, JSONObject jsonObject) {

		Set<Long> taxonomyCategoryIds = new HashSet<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			String label = jsonArray.getString(i);

			String zendeskLabel = _zendeskLabelsMap.get(label);

			if (zendeskLabel == null) {
				if ((label.length() == 2) &&
					!StringUtil.equals(jsonArray.getString(i), "mt")) {

					_addLocalization(jsonObject, jsonArray.getString(i));
				}
				else if (StringUtil.equals(
							label, "Created by the AI Support Assistant")) {

					jsonObject.put(
						"keywords",
						new JSONArray(
						).put(
							label
						));
				}
				else if (StringUtil.extractDigits(
							label
						).length() == label.length()) {

					jsonObject.put(
						"ticketNumber", Long.valueOf(jsonArray.getString(i)));
				}

				continue;
			}

			if (StringUtil.startsWith(zendeskLabel, "DXP ") ||
				StringUtil.startsWith(zendeskLabel, "Portal ")) {

				taxonomyCategoryIds.add(
					Long.valueOf(
						_taxonomyCategories.get("Liferay Self-Hosted")));
			}

			taxonomyCategoryIds.add(
				Long.valueOf(_taxonomyCategories.get(zendeskLabel)));
		}

		return taxonomyCategoryIds;
	}

	private static final Long[] _AUTHOR_IDS = {
		25774534826893L, 373185040091L, 6012626444045L, 407158172372L,
		372705873752L, 402827046671L, 366376802712L, 29923266459277L,
		384097290672L, 372625664892L, 373149011752L, 373184899791L,
		424031032911L, 8281671277197L, 28340825135629L, 385870175971L,
		402999848171L, 373184915451L, 375994556791L, 1903298695907L,
		29178802878861L, 366447371912L, 11438601141133L, 29017365453965L,
		373148995092L, 373860647592L, 20886482355597L, 383194348352L,
		10492726157453L, 419447428432L, 20442344151565L, 1904580184667L,
		373185092851L, 16878347054221L, 385002475392L, 421304055031L,
		372749968591L, 370270860211L, 407158464492L, 17676116379277L,
		366737325611L, 366931880911L, 424277722571L, 424031460891L,
		7578639775629L, 371937132592L, 10179332964237L, 370893685472L,
		403906895791L, 371038218371L, 379745023111L, 10098599333901L,
		373148967792L, 373184985751L, 25968542723085L, 366784167951L,
		20245565000845L, 373148917612L, 373149156712L, 370767034731L,
		366737976392L, 370970624412L, 366871082551L, 372363832491L,
		423037708032L, 377855535812L, 15262410539021L, 366013180451L,
		371091423991L, 372366622452L, 407630088092L, 412413393352L,
		372867984791L, 372923547331L, 6862776677901L, 373185070251L,
		407162261232L, 373035069351L, 373185214891L, 375158712852L,
		389210284951L, 372923546591L, 373185092791L, 421978411212L,
		367411867472L, 373184998571L, 373149066312L, 370623370492L,
		25773897965069L, 373148885832L, 15880641889805L, 373149057092L,
		17531209709581L, 5054395198093L, 5341977284877L, 372528405572L,
		7939158268429L, 372358433692L, 403895380432L, 367119381972L,
		21270730121869L, 373185282371L, 372937222932L, 17676165787917L,
		415192903552L, 371895917152L, 1903130316867L, 415792656351L,
		373185206871L, 19002500942221L, 17581992285709L, 373185220691L,
		373185163871L, 397337294051L, 366918656192L, 367119363532L,
		8281773134349L, 373149024412L, 399572727711L, 13013415006605L,
		373184831871L, 25968541772941L, 373185277551L, 12073805054861L,
		373185298031L, 368020490052L, 399951451392L, 372923547651L,
		392271342791L, 366354951311L, 373185241451L, 373184829291L,
		1902265153247L, 373407136812L, 367157916851L, 373148772232L,
		372999465071L, 405757266912L, 373184952931L, 4529113176717L,
		403307959271L, 372733531471L, 381295176812L, 8671038525709L,
		402826927811L, 373271535911L, 373185276931L, 366926529812L,
		422959208432L, 33003396953613L, 396692389411L, 367107243731L,
		367077572892L, 397592563151L, 366919861412L, 14397070520205L,
		372416455232L, 9807821097869L, 392156252372L, 373184917511L,
		373704979972L, 366925297912L, 422222403031L, 373888844811L,
		27950430947597L, 11729761361421L, 372705728332L, 373148911572L,
		394769506472L, 387901437451L, 373148773852L, 367721768532L,
		394156375352L, 14438092261517L, 18156512534285L, 415532210992L,
		373185141311L, 403906987551L, 366906675192L, 366924943552L,
		11274804041485L, 373184919591L, 391236021471L, 29746765762189L,
		373148799532L, 373378778412L, 385842001352L
	};

	private static final ObjectMapper _objectMapper = new ObjectMapper();

	private final Map<Long, JSONObject> _authorIdMap = new HashMap<>();

	@Value("${liferay.customer.dxp.document.folder.id}")
	private long _documentFolderId;

	@Value("${liferay.osb.spring.boot.client.zendesk.api.token}")
	private String _liferayZendeskApiToken;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${liferay.customer.dxp.site.group.id}")
	private long _siteGroupId;

	private final Map<String, String> _taxonomyCategories = new HashMap<>();
	private final Map<String, String> _zendeskLabelsMap = HashMapBuilder.put(
		"2023.Q3", "DXP 2023.Q3"
	).put(
		"2023.Q4", "DXP 2023.Q4"
	).put(
		"2024.Q1", "DXP 2024.Q1 (LTS)"
	).put(
		"2024.Q2", "DXP 2024.Q2"
	).put(
		"2024.Q3", "DXP 2024.Q3"
	).put(
		"2024.Q4", "DXP 2024.Q4"
	).put(
		"2025.Q1", "DXP 2025.Q1 (LTS)"
	).put(
		"Analytics", "Personalization"
	).put(
		"Analytics Cloud", "Analytics Cloud"
	).put(
		"APIs, Integrations and Extension Points", "Integration"
	).put(
		"Application Security", "Security"
	).put(
		"Backup/Recovery", "Cloud"
	).put(
		"Caching & Clustering", "Platform"
	).put(
		"Calendar", "Sites"
	).put(
		"Collaboration & Document Management", "Digital Asset Management"
	).put(
		"Commerce", "Commerce"
	).put(
		"Commerce", "Liferay Commerce"
	).put(
		"Configuration and Settings", "Platform"
	).put(
		"Connectivity", "Platform"
	).put(
		"Connectors", "Integration"
	).put(
		"Deployment, Environments", "Development and Tooling"
	).put(
		"DXP 7.0", "DXP 7.0"
	).put(
		"DXP 7.1", "DXP 7.1"
	).put(
		"DXP 7.2", "DXP 7.2"
	).put(
		"DXP 7.3", "DXP 7.3"
	).put(
		"DXP 7.4", "DXP 7.4"
	).put(
		"Forms", "Platform"
	).put(
		"Front-end Infrastructure", "Content Management System"
	).put(
		"Liferay API", "Integration"
	).put(
		"LXC", "Cloud"
	).put(
		"LXC", "Liferay SaaS"
	).put(
		"LXC-SM", "Cloud"
	).put(
		"LXC-SM", "Liferay PaaS"
	).put(
		"Monitoring", "Cloud"
	).put(
		"Networking", "Cloud"
	).put(
		"Objects", "Development and Tooling"
	).put(
		"Patching Tool",
		"DXP Self-Hosted Installation, Maintenance, and Administration"
	).put(
		"Performance",
		"DXP Self-Hosted Installation, Maintenance, and Administration"
	).put(
		"Portal 6.1", "Portal 6.1"
	).put(
		"Portal 6.2", "Portal 6.2"
	).put(
		"Search", "Search"
	).put(
		"Segmentation", "Personalization"
	).put(
		"Staging", "Platform"
	).put(
		"Sync", "Platform"
	).put(
		"System Availability", "Cloud"
	).put(
		"Tooling", "Development and Tooling"
	).put(
		"Upgrade",
		"DXP Self-Hosted Installation, Maintenance, and Administration"
	).put(
		"User & System Management",
		"DXP Self-Hosted Installation, Maintenance, and Administration"
	).put(
		"VPN", "Cloud"
	).put(
		"Web Experience Management", "Content Management System"
	).put(
		"Workflow", "Platform"
	).build();
	private final Map<String, String> _zendeskLocaleMap = HashMapBuilder.put(
		"cn", "zh_CN"
	).put(
		"en-us", "en_US"
	).put(
		"es", "es_ES"
	).put(
		"ja", "ja_JP"
	).put(
		"ko", "ko_KR"
	).put(
		"pt", "pt_BR"
	).put(
		"zh-cn", "zh_CN"
	).build();

	@Value("liferay.osb.spring.boot.client.zendesk.url")
	private String _zendeskUrl;

}